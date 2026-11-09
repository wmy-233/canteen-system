package com.canteen.mapper;

import com.canteen.entity.WalletRecord;
import com.canteen.vo.WalletRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 钱包流水 Mapper
 */
@Mapper
public interface WalletRecordMapper {

    int insert(WalletRecord record);

    List<WalletRecordVO> selectPageByUserId(@Param("userId") Long userId);
}
