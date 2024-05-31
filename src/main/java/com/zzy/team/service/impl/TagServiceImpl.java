package com.zzy.team.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzy.team.model.domain.Tag;
import com.zzy.team.service.TagService;
import com.zzy.team.mapper.TagMapper;
import org.springframework.stereotype.Service;

/**
* @author zzy
* @description 针对表【tag(标签表)】的数据库操作Service实现
* @createDate 2024-05-30 17:07:28
*/
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag>
    implements TagService{

}




