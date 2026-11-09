package com.canteen.service.impl;

import com.canteen.common.BizException;
import com.canteen.common.PageResult;
import com.canteen.dto.DishSaveDTO;
import com.canteen.entity.Dish;
import com.canteen.entity.Stall;
import com.canteen.mapper.DishMapper;
import com.canteen.mapper.ReviewMapper;
import com.canteen.mapper.StallMapper;
import com.canteen.service.DishService;
import com.canteen.service.RedisStockService;
import com.canteen.service.RedisSupport;
import com.canteen.vo.DishVO;
import com.canteen.vo.SaleRankVO;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScoredSortedSet;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜品实现（FR-STU-01 / FR-MER-02 / FR-STU-10 热销榜）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DishServiceImpl implements DishService {

    /** 热销榜 Redis ZSet key（累计销量口径，O-3 决议） */
    public static final String RANK_KEY = "rank:sale";

    private final DishMapper dishMapper;
    private final StallMapper stallMapper;
    private final ReviewMapper reviewMapper;
    private final RedisSupport redisSupport;
    private final RedisStockService redisStockService;

    @Override
    public PageResult<DishVO> pageForStudent(Long canteenId, Long stallId, Long categoryId,
                                             String keyword, Integer pageNum, Integer pageSize) {
        pageNum = pageNum == null ? 1 : pageNum;
        pageSize = pageSize == null ? 10 : pageSize;
        PageHelper.startPage(pageNum, pageSize);
        List<DishVO> list = dishMapper.selectPage(canteenId, stallId, categoryId, keyword, null, true);
        fillAvgScore(list);
        return PageResult.build(list, pageNum, pageSize);
    }

    @Override
    public PageResult<DishVO> pageForMerchant(Long merchantUserId, Long stallId, String keyword,
                                              Integer pageNum, Integer pageSize) {
        // 数据权限：商家只能查自己档口（merchantUserId 直接截断查询范围）
        pageNum = pageNum == null ? 1 : pageNum;
        pageSize = pageSize == null ? 10 : pageSize;
        PageHelper.startPage(pageNum, pageSize);
        List<DishVO> list = dishMapper.selectPage(null, stallId, null, keyword, merchantUserId, false);
        fillAvgScore(list);
        return PageResult.build(list, pageNum, pageSize);
    }

    @Override
    public DishVO detail(Long id) {
        DishVO dish = dishMapper.selectByIds(List.of(id)).stream().findFirst()
                .orElseThrow(() -> new BizException("菜品不存在"));
        fillAvgScore(List.of(dish));
        return dish;
    }

    @Override
    public void save(Long merchantUserId, DishSaveDTO dto) {
        // 商家仅本档口（数据权限隔离，越权直接拒绝）
        Stall stall = stallMapper.selectById(dto.getStallId());
        if (stall == null) {
            throw new BizException("档口不存在");
        }
        if (!stall.getMerchantUserId().equals(merchantUserId)) {
            log.warn("菜品越权操作被拦截: stallId={}, operator={}", dto.getStallId(), merchantUserId);
            throw new BizException("只能操作本档口菜品");
        }
        Dish dish = new Dish();
        dish.setStallId(dto.getStallId());
        dish.setCategoryId(dto.getCategoryId());
        dish.setName(dto.getName());
        dish.setDescription(dto.getDescription());
        dish.setPrice(dto.getPrice());
        dish.setImage(dto.getImage());
        dish.setDailyStock(dto.getDailyStock() == null ? 0 : dto.getDailyStock());
        dish.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        if (dto.getId() == null) {
            dishMapper.insert(dish);
            log.info("菜品新增: id={}, name={}, stallId={}, operator={}",
                    dish.getId(), dish.getName(), dish.getStallId(), merchantUserId);
        } else {
            Dish exist = dishMapper.selectById(dto.getId());
            if (exist == null || !exist.getStallId().equals(dto.getStallId())) {
                throw new BizException("菜品不存在或不属于本档口");
            }
            dish.setId(dto.getId());
            dishMapper.updateSelective(dish);
            // 修改每日限量后清 Redis 余量缓存，下次下单按新值初始化
            redisStockService.evict(dto.getId());
            log.info("菜品修改: id={}, name={}, operator={}", dish.getId(), dish.getName(), merchantUserId);
        }
    }

    @Override
    public void delete(Long merchantUserId, Long id) {
        Dish dish = dishMapper.selectById(id);
        if (dish == null) {
            throw new BizException("菜品不存在");
        }
        Stall stall = stallMapper.selectById(dish.getStallId());
        if (stall == null || !stall.getMerchantUserId().equals(merchantUserId)) {
            log.warn("菜品删除越权被拦截: dishId={}, operator={}", id, merchantUserId);
            throw new BizException("只能操作本档口菜品");
        }
        int rows = dishMapper.delete(id);
        if (rows == 0) {
            throw new BizException("菜品不存在或已删除");
        }
        redisStockService.evict(id);
        log.info("菜品删除: id={}, operator={}", id, merchantUserId);
    }

    @Override
    public void changeSaleStatus(Long merchantUserId, Long id, Integer status) {
        Dish dish = dishMapper.selectById(id);
        if (dish == null) {
            throw new BizException("菜品不存在");
        }
        Stall stall = stallMapper.selectById(dish.getStallId());
        if (stall == null || !stall.getMerchantUserId().equals(merchantUserId)) {
            log.warn("菜品上下架越权被拦截: dishId={}, operator={}", id, merchantUserId);
            throw new BizException("只能操作本档口菜品");
        }
        Dish update = new Dish();
        update.setId(id);
        update.setStatus(status);
        dishMapper.updateSelective(update);
        log.info("菜品上下架: id={}, status={}, operator={}", id, status, merchantUserId);
    }

    @Override
    public List<SaleRankVO> hotRank(int limit) {
        if (redisSupport.isAvailable()) {
            try {
                // 亮点 H2：Redis ZSet 累计销量实时榜（下单 addScore，读侧倒序取 TopN）
                RScoredSortedSet<String> rank = redisSupport.getClient().getScoredSortedSet(RANK_KEY);
                Collection<String> topMembers = rank.valueRangeReversed(0, Math.max(limit - 1, 0));
                List<Long> ids = new ArrayList<>();
                for (String member : topMembers) {
                    ids.add(Long.valueOf(member));
                }
                if (!ids.isEmpty()) {
                    Map<Long, DishVO> dishMap = dishMapper.selectByIds(ids).stream()
                            .collect(Collectors.toMap(DishVO::getId, d -> d));
                    List<SaleRankVO> result = new ArrayList<>();
                    for (Long id : ids) {
                        DishVO dish = dishMap.get(id);
                        if (dish != null) {
                            SaleRankVO vo = new SaleRankVO();
                            vo.setDishId(id);
                            vo.setName(dish.getName());
                            vo.setSaleCount(dish.getSaleCount());
                            result.add(vo);
                        }
                    }
                    return result;
                }
            } catch (Exception e) {
                log.warn("热销榜读取异常，降级为数据库销量排序: {}", e.getMessage());
            }
        }
        // 降级：数据库 sale_count 排序
        return topFromDatabase(limit);
    }

    private List<SaleRankVO> topFromDatabase(int limit) {
        List<Dish> dishes = dishMapper.selectTopSale(limit);
        List<SaleRankVO> result = new ArrayList<>();
        for (Dish dish : dishes) {
            SaleRankVO vo = new SaleRankVO();
            vo.setDishId(dish.getId());
            vo.setName(dish.getName());
            vo.setSaleCount(dish.getSaleCount());
            result.add(vo);
        }
        return result;
    }

    /** 评价聚合填充 1-5 评分均值 */
    private void fillAvgScore(List<DishVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> ids = list.stream().map(DishVO::getId).collect(Collectors.toList());
        Map<Long, Double> avgMap = new HashMap<>();
        for (Map<String, Object> row : reviewMapper.selectAvgByDishIds(ids)) {
            Long dishId = ((Number) row.get("dishId")).longValue();
            Object score = row.get("avgScore");
            if (score != null) {
                avgMap.put(dishId, Double.valueOf(score.toString()));
            }
        }
        for (DishVO vo : list) {
            vo.setAvgScore(avgMap.get(vo.getId()));
        }
    }
}
