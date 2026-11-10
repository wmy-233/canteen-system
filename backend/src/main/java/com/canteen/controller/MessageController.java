package com.canteen.controller;

import com.canteen.common.PageResult;
import com.canteen.common.Result;
import com.canteen.common.SecurityUtil;
import com.canteen.entity.SysMessage;
import com.canteen.mapper.SysMessageMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 站内消息中心（登录即可访问，三端共用）
 */
@Tag(name = "站内消息")
@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
public class MessageController {

    private final SysMessageMapper sysMessageMapper;

    @Operation(summary = "消息分页（isRead 不传=全部，传0=未读）")
    @GetMapping("/page")
    public Result<PageResult<SysMessage>> page(@RequestParam(required = false) Integer isRead,
                                               @RequestParam(defaultValue = "1") Integer pageNum,
                                               @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = SecurityUtil.getUserId();
        pageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        pageSize = pageSize == null || pageSize < 1 || pageSize > 50 ? 10 : pageSize;
        List<SysMessage> list = sysMessageMapper.selectPage(userId, isRead, (pageNum - 1) * pageSize, pageSize);
        long total = sysMessageMapper.countPage(userId, isRead);
        return Result.success(PageResult.build(list, pageNum, pageSize));
    }

    @Operation(summary = "未读数量（顶栏铃铛角标轮询用）")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.success(sysMessageMapper.countUnread(SecurityUtil.getUserId()));
    }

    @Operation(summary = "标记单条已读")
    @PostMapping("/{id}/read")
    public Result<Void> read(@PathVariable Long id) {
        sysMessageMapper.markRead(id, SecurityUtil.getUserId());
        return Result.success();
    }

    @Operation(summary = "全部标记已读")
    @PostMapping("/read-all")
    public Result<Void> readAll() {
        sysMessageMapper.markAllRead(SecurityUtil.getUserId());
        return Result.success();
    }
}
