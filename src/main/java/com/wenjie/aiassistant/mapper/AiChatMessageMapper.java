package com.wenjie.aiassistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wenjie.aiassistant.entity.AiChatMessageEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiChatMessageMapper
        extends BaseMapper<AiChatMessageEntity> {
}
