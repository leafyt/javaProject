package com.rzspider.project.blog.blogset.controller;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.tomcat.util.http.fileupload.FileUploadBase.FileSizeLimitExceededException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.rzspider.project.blog.blogset.domain.Blogset;
import com.rzspider.project.blog.blogset.domain.Blogsiderbar;
import com.rzspider.project.blog.blogset.service.IBlogsetService;
import com.rzspider.project.blog.blogset.service.IBlogsiderbarService;
import com.rzspider.project.common.file.utilt.FileUtils;
import com.rzspider.project.system.role.service.IRoleService;
import com.rzspider.project.system.website.domain.Website;
import com.rzspider.common.constant.CommonConstant;
import com.rzspider.common.constant.CommonSymbolicConstant;
import com.rzspider.common.constant.FileExtensionConstant;
import com.rzspider.common.constant.FileMessageConstant;
import com.rzspider.common.constant.FileOtherConstant;
import com.rzspider.common.constant.OtherConstant;
import com.rzspider.common.constant.ReturnMessageConstant;
import com.rzspider.common.constant.UserConstants;
import com.rzspider.common.constant.project.BlogConstant;
import com.rzspider.common.utils.FileUploadUtils;
import com.rzspider.common.utils.OtherUtils;
import com.rzspider.common.utils.security.ShiroUtils;
import com.rzspider.framework.aspectj.lang.annotation.Log;
import com.rzspider.framework.config.FilePathConfig;
import com.rzspider.framework.web.controller.BaseController;
import com.rzspider.framework.web.page.TableDataInfo;
import com.rzspider.framework.web.domain.Message;

/**
 * ?????????????????? ??????????????????
 * 
 * @author ricozhou
 * @date 2018-09-13
 */
@Controller
@RequestMapping("/blog/blogset")
public class BlogsetController extends BaseController {
	private String prefix = "blog/blogset";

	@Autowired
	private IBlogsetService blogsetService;

	@Autowired
	private IBlogsiderbarService blogsiderbarService;
	@Autowired
	private IRoleService roleService;

	@GetMapping()
	@RequiresPermissions("blog:blogset:view")
	public String blogset(Model model) {
		List<Blogset> list = blogsetService.selectBlogsetList(null);
		if (list != null && list.size() > 0) {
			model.addAttribute("blogset", list.get(0));
		}
		// ????????????????????????
		List<Blogsiderbar> blogsiderbarList = blogsiderbarService.selectBlogsiderbarList(null);
		model.addAttribute("blogsiderbarList", blogsiderbarList);
		return prefix + "/blogset";
	}

	/**
	 * ??????????????????????????????
	 */
	@RequiresPermissions("blog:blogset:save")
	@PostMapping("/saveBasicset")
	@ResponseBody
	public Message saveBasicset(Blogset blogset) {
		// ?????????????????????
		if (OtherUtils.isTestManager(roleService.selectRoleKeys(ShiroUtils.getUserId()))) {
			return Message.error(UserConstants.USER_MESSAGE_TEATADMIN_DISABLED_USE);
		}
		if (blogsetService.saveBasicset(blogset) > 0) {
			return Message.success();
		}
		return Message.error(BlogConstant.BLOG_BASICSET_SAVE_FAILED);
	}

	/**
	 * ????????????????????????
	 */
	@RequiresPermissions("blog:blogset:save")
	@PostMapping("/saveBloggerset")
	@ResponseBody
	public Message saveBloggerset(Blogset blogset) {
		// ?????????????????????
		if (OtherUtils.isTestManager(roleService.selectRoleKeys(ShiroUtils.getUserId()))) {
			return Message.error(UserConstants.USER_MESSAGE_TEATADMIN_DISABLED_USE);
		}
		if (blogsetService.saveBloggerset(blogset) > 0) {
			return Message.success();
		}
		return Message.error(BlogConstant.BLOG_BLOGGERSET_SAVE_FAILED);
	}

	/**
	 * ????????????????????????
	 */
	@RequiresPermissions("blog:blogset:save")
	@PostMapping("/saveBlogset")
	@ResponseBody
	public Message saveBlogset(Blogset blogset) {
		// ?????????????????????
		if (OtherUtils.isTestManager(roleService.selectRoleKeys(ShiroUtils.getUserId()))) {
			return Message.error(UserConstants.USER_MESSAGE_TEATADMIN_DISABLED_USE);
		}
		if (blogsetService.saveBlogset(blogset) > 0) {
			return Message.success();
		}
		return Message.error(BlogConstant.BLOG_BLOGSET_SAVE_FAILED);
	}

	/**
	 * ????????????????????????
	 */
	@RequiresPermissions("blog:blogset:save")
	@PostMapping("/saveStyleset")
	@ResponseBody
	public Message saveStyleset(Blogset blogset) {
		// ?????????????????????
		if (OtherUtils.isTestManager(roleService.selectRoleKeys(ShiroUtils.getUserId()))) {
			return Message.error(UserConstants.USER_MESSAGE_TEATADMIN_DISABLED_USE);
		}
		if (blogsetService.saveStyleset(blogset) > 0) {
			return Message.success();
		}
		return Message.error(BlogConstant.BLOG_BLOGSET_SAVE_FAILED);
	}

	/**
	 * ???????????????????????????????????????????????????
	 */
	@Log(title = "????????????", action = "????????????-????????????")
	@ResponseBody
	@PostMapping("/uploadImgFile")
	public Message uploadFile(@RequestParam("file") MultipartFile file, HttpServletRequest request, String blogFileName)
			throws IOException {
		String basePath = FilePathConfig.getUploadBlogPath();
		// ?????????????????????????????????
		try {
			FileUploadUtils.assertAllowedSetSize(file, 2 * 1024 * 1024);
		} catch (FileSizeLimitExceededException e1) {
			e1.printStackTrace();
			return Message.error(FileMessageConstant.FILE_MESSAGE_SIZE_GREATER_SIZE);
		}
		// ????????????
		String fileName = file.getOriginalFilename();

		// ?????????
		fileName = FileUploadUtils.renameToUUID(fileName);
		if (blogFileName == null || CommonSymbolicConstant.EMPTY_STRING.equals(blogFileName)
				|| CommonConstant.UNDEFINED.equals(blogFileName)) {
			blogFileName = "blogset";
		} else {
			// ?????? blogFileName
			
		}
		// ?????????
		try {
			FileUploadUtils.uploadFile(file.getBytes(), basePath + File.separator + blogFileName, fileName);
		} catch (Exception e) {
			return Message.error();
		}
		// String imgbase64String = OtherConstant.OTHER_DATAIMAGE
		// +
		// fileName.substring(fileName.lastIndexOf(CommonSymbolicConstant.POINT)
		// + 1)
		// + OtherConstant.OTHER_BASE64 + new
		// String(Base64.encodeBase64(file.getBytes()));
		// ????????????????????????
		String imgbase64String = FileOtherConstant.FILE_JUMP_PATH_PREFIX3 + blogFileName + File.separator + fileName;
		Message message = new Message();
		message = message.success();
		message.put(ReturnMessageConstant.RETURN_MESSAGE_KEY_1, imgbase64String);
		// message.put(ReturnMessageConstant.RETURN_MESSAGE_KEY_2,
		// fileName);
		return message;

	}

}
