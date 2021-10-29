package com.can.cblog.admin;

import com.can.cblog.xo.service.AdminService;
import com.can.cblog.xo.vo.AdminVO;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ccc
 */
@SpringBootTest
public class TestAdmin {

    @Autowired
    AdminService adminService;

    @Test
    public void testDeleteAdmin() {
        List<String> adminUidList = new ArrayList<>();
        adminUidList.add("7621746caa93ce605e2de7143a3787b5");
        //adminService.deleteBatchAdmin(adminUidList);
        AdminVO adminVO = new AdminVO();
        System.out.println(adminService.getList(adminVO));

    }
}
