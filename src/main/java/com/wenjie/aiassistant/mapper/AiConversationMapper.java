package com.wenjie.aiassistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wenjie.aiassistant.entity.AiConversationEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiConversationMapper
        extends BaseMapper<AiConversationEntity> {
}