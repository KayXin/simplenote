package com.xiaoyu.modules.common.service.api;

import java.util.List;

import com.xiaoyu.modules.common.entity.HostRecord;

/**
 * @author hongyu
 * @date 2018-08
 * @description
 */
public interface IHostService {

    public void queryLocation();

    public int batchInsert(List<HostRecord> hostCache);
}
