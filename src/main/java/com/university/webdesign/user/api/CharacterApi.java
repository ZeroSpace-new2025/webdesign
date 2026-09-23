package com.university.webdesign.user.api;

/*其他的API接口可以根据需要添加。修改后要立即提交。*/
/*应该只包含与角色相关的API接口，而不应该包含与其他相关的接口。*/
/*请勿把内部实现暴露给外部。*/

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 角色与权限 API
 * <p>
 * 对应需求中的“角色管理：定义餐厅经理、厨房主管、配餐员、财务、员工等角色”。
 * //todo 确认：本类原先只是一个空类（类名 Character 按需求表述应为 Role），
 * 目前仅补上 {@code @RestController} 骨架，接口方法与是否改名为 {@code RoleApi}
 * 需与用户与报表中心（王家豪）确认后再实现。
 */
@RestController
@RequestMapping("/api/character")
public class CharacterApi
{
	//crud：角色增删查改、角色与权限映射维护
}
