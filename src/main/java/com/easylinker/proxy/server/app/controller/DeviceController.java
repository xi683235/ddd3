package com.easylinker.proxy.server.app.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.easylinker.proxy.server.app.model.device.Device;
import com.easylinker.proxy.server.app.constants.result.ReturnResult;
import com.easylinker.proxy.server.app.model.user.AppUser;
import com.easylinker.proxy.server.app.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 设备操作业务逻辑
 */
@RestController
@RequestMapping("/device")
@PreAuthorize(value = "hasRole('ROLE_USER') OR hasRole('ROLE_ADMIN')")
public class DeviceController {
    @Autowired
    DeviceService deviceService;
    @Autowired
    DeviceDataService deviceDataService;
    @Autowired
    DeviceGroupService deviceGroupService;

    @Autowired
    DeviceOnAndOffLineLogService deviceOnAndOffLineLogService;


    /**
     * 获取单个设备的细节
     *
     * @return
     */
    @RequestMapping(value = "/getDeviceDetail/{deviceId}", method = RequestMethod.GET)
    public JSONObject getDeviceDetail(@PathVariable Long deviceId) {

        AppUser appUser = (AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Device device = deviceService.findADevice(deviceId);
        if (device != null && device.getAppUser().getId().longValue() == appUser.getId().longValue()) {

            return ReturnResult.returnDataMessage(1, "查询成功!", deviceService.getDeviceDetail(deviceId));
        } else {
            return ReturnResult.returnTipMessage(0, "设备不存在!");
        }


    }

    /**
     * 获取设备产生的数据
     *
     * @return
     */
    @RequestMapping(value = "/getDeviceData/{deviceId}/{page}/{size}", method = RequestMethod.GET)
    public JSONObject getDeviceData(@PathVariable Long deviceId, @PathVariable int page, @PathVariable int size) {
        Device device = deviceService.findADevice(deviceId);
        if (device == null)
            return ReturnResult.returnTipMessage(0, "设备不存在!");

        if (device.getAppUser() == null)
            return ReturnResult.returnTipMessage(0, "设备未绑定!");
        if (device != null) {
            JSONArray data = deviceDataService.getAllDeviceDataByDevice(device, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime")));

            return ReturnResult.returnDataMessage(1, "查询成功!", data);

        } else {
            return ReturnResult.returnTipMessage(0, "设备不存在!");
        }

    }


    /**
     * 分页获取设备日志
     */
    @RequestMapping(value = "/getAllLogByPage/{deviceId}/{page}/{size}", method = RequestMethod.GET)
    public JSONObject getAllLogByPage(@PathVariable Long deviceId, @PathVariable int page, @PathVariable int size) {
        Device device = deviceService.findADevice(deviceId);
        if (device == null)
            return ReturnResult.returnTipMessage(0, "设备不存在!");

        if (device.getAppUser() == null)
            return ReturnResult.returnTipMessage(0, "设备未绑定!");
        if (device != null) {

            return ReturnResult.returnDataMessage(1, "查询成功!",
                    deviceOnAndOffLineLogService.getAllLogByPage(
                            device, PageRequest.of(page, size,
                                    Sort.by(Sort.Direction.DESC,
                                            "id"))));

        } else {
            return ReturnResult.returnTipMessage(0, "设备不存在!");
        }
    }

    /**
     * 分页获取所有设备日志

     */
    @RequestMapping(value = "/getAllLog/{page}/{size}", method = RequestMethod.GET)
    public JSONObject getAllLog(@PathVariable int page, @PathVariable int size) {
        AppUser appUser = (AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        for (GrantedAuthority authority:appUser.getAuthorities()) {
            if ("ROLE_ADMIN".equals(authority.getAuthority()))
                return ReturnResult.returnDataMessage(1, "查询成功!", deviceOnAndOffLineLogService.getAllLog(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"))));
        }
        return ReturnResult.returnDataMessage(1, "查询成功!", deviceOnAndOffLineLogService.getAllLogByUser(appUser,PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"))));
    }





}
