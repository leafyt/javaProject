package com.rzspider.project.spider.customspider.controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.rzspider.common.constant.CommonSymbolicConstant;
import com.rzspider.common.constant.FileExtensionConstant;
import com.rzspider.common.constant.FileMessageConstant;
import com.rzspider.common.constant.UserConstants;
import com.rzspider.common.constant.project.SpiderConstant;
import com.rzspider.common.constant.project.SpiderMessageConstant;
import com.rzspider.common.utils.FileUploadUtils;
import com.rzspider.common.utils.OtherUtils;
import com.rzspider.common.utils.StringUtils;
import com.rzspider.common.utils.security.ShiroUtils;
import com.rzspider.framework.aspectj.lang.annotation.Log;
import com.rzspider.framework.config.FilePathConfig;
import com.rzspider.framework.web.controller.BaseController;
import com.rzspider.framework.web.domain.Message;
import com.rzspider.framework.web.page.TableDataInfo;
import com.rzspider.project.common.file.utilt.FileUtils;
import com.rzspider.project.commontool.toolmanage.domain.Toolmanage;
import com.rzspider.project.spider.codeType.domain.CodeType;
import com.rzspider.project.spider.codeType.mapper.CodeTypeMapper;
import com.rzspider.project.spider.customspider.domain.Customspider;
import com.rzspider.project.spider.customspider.domain.CustomspiderBackupcode;
import com.rzspider.project.spider.customspider.domain.FileTree;
import com.rzspider.project.spider.customspider.service.ICSFileService;
import com.rzspider.project.spider.customspider.service.ICustomspiderBackupcodeService;
import com.rzspider.project.spider.customspider.service.ICustomspiderService;
import com.rzspider.project.spider.customspider.service.IFileTreeService;
import com.rzspider.project.spider.customspider.utils.BaseCSUtils;
import com.rzspider.project.system.role.service.IRoleService;
import com.rzspider.project.tool.baseset.service.IBasesetService;

/**
 * ?????????????????????????????????
 * 
 * @author rico
 * @date 2018-06-01
 */
@Controller
@RequestMapping("/spider/customspider/code")
public class CSFileController extends BaseController {
	private String prefix = "spider/customspider/code";

	@Autowired
	private ICSFileService cSFileService;
	@Autowired
	private ICustomspiderService customspiderService;
	@Autowired
	private IFileTreeService fileTreeService;
	@Autowired
	private IBasesetService basesetService;
	@Autowired
	private CodeTypeMapper codeTypeMapper;
	@Autowired
	private IRoleService roleService;

	/**
	 * ?????????????????????
	 */
	@GetMapping("/treeData")
	@ResponseBody
	public List<Map<String, Object>> treeData(Integer customSpiderId) {
		List<Map<String, Object>> tree = cSFileService.selectCSFileTree(customSpiderId);
		return tree;
	}

	/**
	 * ???????????????????????????
	 * 
	 * @param customspider
	 * @return
	 */
	@Log(title = "????????????", action = "???????????????-??????????????????")
	@RequiresPermissions("spider:customspider:code:openFile")
	@PostMapping("/getFile")
	@ResponseBody
	public FileTree getFile(FileTree fileTree) {
		if (fileTree != null) {
			fileTree = cSFileService.getFileContent(fileTree);
			if (fileTree != null) {
				fileTree.setStatus(0);
			}
		}
		return fileTree;
	}

	/**
	 * ?????????????????????????????????
	 */
	@GetMapping("/newFile/{flag}/{customSpiderId}/{childId}")
	public String newFile(@PathVariable("flag") Integer flag, @PathVariable("customSpiderId") Integer customSpiderId,
			@PathVariable("childId") Integer childId, Model model) {
		model.addAttribute("customSpiderId", customSpiderId);
		model.addAttribute("childId", childId);
		if (flag == 0) {
			return prefix + "/newJavaFile";
		} else if (flag == 1 || flag == 2) {
			model.addAttribute("flag", flag);
			return prefix + "/newJSPYFile";
		}
		return prefix + "/newFile";
	}

	/**
	 * ????????????
	 */
	@GetMapping("/installPackage/{flag}/{customSpiderId}/{childId}")
	public String installPackage(@PathVariable("flag") Integer flag,
			@PathVariable("customSpiderId") Integer customSpiderId, @PathVariable("childId") Integer childId,
			Model model) {
		model.addAttribute("flag", flag);
		model.addAttribute("customSpiderId", customSpiderId);
		model.addAttribute("childId", childId);
		return prefix + "/installPackage";
	}

	/**
	 * ?????????????????????
	 */
	@Log(title = "????????????", action = "???????????????-????????????java")
	@Transactional(rollbackFor = Exception.class)
	@RequiresPermissions("spider:customspider:code:fileSave")
	@PostMapping("/saveFile")
	@ResponseBody
	public Message saveFile(FileTree fileTree) {
		// ?????????????????????
		if (OtherUtils.isTestManager(roleService.selectRoleKeys(ShiroUtils.getUserId()))) {
			return Message.error(UserConstants.USER_MESSAGE_TEATADMIN_DISABLED_USE);
		}

		if (cSFileService.saveFile(fileTree)) {
			return Message.success();
		}
		return Message.error(SpiderMessageConstant.SPIDER_MESSAGE_JAVASAVECOMPILE_FAILED);
	}

	/**
	 * ??????????????????
	 */
	@Log(title = "????????????", action = "???????????????-????????????java")
	@RequiresPermissions("spider:customspider:code:newFile")
	@PostMapping("/newJavaFileSave")
	@ResponseBody
	public Message newJavaFileSave(FileTree fileTree) {
		// ?????????????????????
		if (OtherUtils.isTestManager(roleService.selectRoleKeys(ShiroUtils.getUserId()))) {
			return Message.error(UserConstants.USER_MESSAGE_TEATADMIN_DISABLED_USE);
		}
		// ??????id???childid???????????????????????????????????????
		FileTree fileTree2 = fileTreeService.selectFileTreeByCSIdAndChildId(fileTree);
		fileTree.setFilePath(fileTree2.getFilePath());
		fileTree.setCustomSpiderBackId(fileTree2.getCustomSpiderBackId());
		// ????????????
		if (!cSFileService.checkClassNameAndRepeated(fileTree)) {
			return Message.error(SpiderMessageConstant.SPIDER_MESSAGE_JAVACLASSNAME_ILLEGAL_EXIST);
		}

		if (cSFileService.newJavaFileSave(fileTree)) {
			return Message.success();
		}
		return Message.error(FileMessageConstant.FILE_MESSAGE_FILE_CREATE_FAILED);
	}

	/**
	 * ??????????????????
	 */
	@Log(title = "????????????", action = "???????????????-????????????Python???js")
	@RequiresPermissions("spider:customspider:code:newFile")
	@PostMapping("/newJSPYFileSave")
	@ResponseBody
	public Message newJSPYFileSave(FileTree fileTree) {
		// ?????????????????????
		if (OtherUtils.isTestManager(roleService.selectRoleKeys(ShiroUtils.getUserId()))) {
			return Message.error(UserConstants.USER_MESSAGE_TEATADMIN_DISABLED_USE);
		}
		// ??????id???childid???????????????????????????????????????
		FileTree fileTree2 = fileTreeService.selectFileTreeByCSIdAndChildId(fileTree);
		fileTree.setFilePath(fileTree2.getFilePath());
		fileTree.setCustomSpiderBackId(fileTree2.getCustomSpiderBackId());
		// ????????????
		if (!cSFileService.checkRepeated(fileTree)) {
			return Message.error(SpiderMessageConstant.SPIDER_MESSAGE_NEWFILE_ILLEGAL_EXIST);
		}

		if (cSFileService.newJSPYFileSave(fileTree)) {
			return Message.success();
		}
		return Message.error(FileMessageConstant.FILE_MESSAGE_FILE_CREATE_FAILED);
	}

	/**
	 * ?????????????????????
	 */
	@Log(title = "????????????", action = "???????????????-??????????????????")
	@RequiresPermissions("spider:customspider:code:importFile")
	@PostMapping("/installPackageSave")
	@ResponseBody
	public Message installPackageSave(FileTree fileTree) {
		// ?????????????????????
		if (OtherUtils.isTestManager(roleService.selectRoleKeys(ShiroUtils.getUserId()))) {
			return Message.error(UserConstants.USER_MESSAGE_TEATADMIN_DISABLED_USE);
		}
		// ??????id???childid???????????????????????????????????????
		FileTree fileTree2 = fileTreeService.selectFileTreeByCSIdAndChildId(fileTree);
		if (fileTree.getFlag() == 0) {
			// python
			fileTree.setFilePath(
					fileTree2.getFilePath() + File.separator + SpiderConstant.SPIDER_PYTHON_DEPENDENCY_PACKAGE_FOLDER);
		} else if (fileTree.getFlag() == 1) {
			// js
			 fileTree.setFilePath(fileTree2.getFilePath());
		}
		fileTree.setCustomSpiderBackId(fileTree2.getCustomSpiderBackId());

		String[] returnContent = cSFileService.installPackageSave(fileTree);
		if (returnContent != null && returnContent[0] != null) {
			return Message.success();
		}
		return Message.error(SpiderMessageConstant.SPIDER_MESSAGE_INSTALL_PACKAGE_FAILED);
	}

	/**
	 * ????????????????????????
	 */
	@Log(title = "????????????", action = "???????????????-??????java??????")
	@PostMapping("/checkClassName")
	@ResponseBody
	public Message checkClassName(FileTree fileTree) {
		if (cSFileService.checkClassName(fileTree)) {
			return Message.success();
		}
		return Message.error(SpiderMessageConstant.SPIDER_MESSAGE_JAVACLASSNAME_ILLEGAL);
	}

	/**
	 * ??????????????????
	 */
	@Log(title = "????????????", action = "???????????????-????????????????????????")
	@RequiresPermissions("spider:customspider:code:run")
	@Transactional(rollbackFor = Exception.class)
	@PostMapping("/run")
	@ResponseBody
	public FileTree run(FileTree fileTree) {
		// ?????????????????????
		if (OtherUtils.isTestManager(roleService.selectRoleKeys(ShiroUtils.getUserId()))) {
			fileTree.setStatus(4);
			return fileTree;
		}
		// ????????????
		// ?????????????????????
		Customspider customspider = customspiderService
				.selectCustomspiderByCustomSpiderBackId(fileTree.getCustomSpiderBackId());
		// ????????????????????????
		if (customspider.getRunStatus() == 0) {
			// ????????????
			fileTree.setStatus(3);
			return fileTree;
		}
		fileTree.setSpiderCodeTypeFolder(customspider.getSpiderCodeTypeFolder());
		String returnPrintContent = null;
		if (customspider.getCustomSpiderType() == 0) {
			// java??????
			// ???????????????????????????main??????
			if (!cSFileService.checkJavaFile(fileTree,
					StringUtils.getNotNullString(customspider.getSpiderJavaPackagePrefix()))) {
				fileTree.setStatus(1);// ????????????main??????
				return fileTree;
			}

			if (!cSFileService.runJavaCode(fileTree,
					StringUtils.getNotNullString(customspider.getSpiderJavaPackagePrefix()),
					StringUtils.getNotNullString(customspider.getSpiderDefaultParams()))) {
				fileTree.setStatus(2);
				return fileTree;
			}
		} else if (customspider.getCustomSpiderType() == 1) {
			// python??????
			if (!cSFileService.runPythonCode(fileTree,
					StringUtils.getNotNullString(customspider.getSpiderJavaPackagePrefix()),
					StringUtils.getNotNullString(customspider.getSpiderDefaultParams()))) {
				fileTree.setStatus(2);
				return fileTree;
			}
		} else if (customspider.getCustomSpiderType() == 2) {
			// javascript??????
			if (!cSFileService.runJSCode(fileTree,
					StringUtils.getNotNullString(customspider.getSpiderJavaPackagePrefix()),
					StringUtils.getNotNullString(customspider.getSpiderDefaultParams()))) {
				fileTree.setStatus(2);
				return fileTree;
			}
		} else if (customspider.getCustomSpiderType() == 3) {
			// jar???
			if (!cSFileService.runJAR(fileTree, StringUtils.getNotNullString(customspider.getSpiderDefaultParams()))) {
				fileTree.setStatus(2);
				return fileTree;
			}
		}
		// ???????????????????????????????????????,??????????????????
		fileTree.setStatus(0);
		return fileTree;

	}

	/**
	 * ??????????????????
	 */
	@Log(title = "????????????", action = "???????????????-????????????????????????")
	@RequiresPermissions("spider:customspider:code:run")
	@Transactional(rollbackFor = Exception.class)
	@PostMapping("/stop")
	@ResponseBody
	public FileTree stop(FileTree fileTree) {
		// ?????????????????????
		if (OtherUtils.isTestManager(roleService.selectRoleKeys(ShiroUtils.getUserId()))) {
			fileTree.setStatus(4);
			return fileTree;
		}
		if (!cSFileService.stopCSProcess(fileTree)) {
			fileTree.setStatus(1);// ????????????
			return fileTree;
		}
		fileTree.setStatus(0);
		return fileTree;

	}

	/**
	 * ????????????
	 */
	@Log(title = "????????????", action = "???????????????-????????????")
	@RequiresPermissions("spider:customspider:code:deleteFile")
	@PostMapping("/deleteFile")
	@ResponseBody
	public Message deleteFile(FileTree fileTree) {
		// ?????????????????????
		if (OtherUtils.isTestManager(roleService.selectRoleKeys(ShiroUtils.getUserId()))) {
			return Message.error(UserConstants.USER_MESSAGE_TEATADMIN_DISABLED_USE);
		}
		// ??????id???childid???????????????????????????????????????
		FileTree fileTree2 = fileTreeService.selectFileTreeByCSIdAndChildId(fileTree);
		fileTree.setFilePath(fileTree2.getFilePath());
		fileTree.setCustomSpiderBackId(fileTree2.getCustomSpiderBackId());
		File file = new File(fileTree.getFilePath());
		if (!file.exists()) {
			return Message.error(FileMessageConstant.FILE_MESSAGE_FILE_NO_EXIST);
		}
		if (cSFileService.deleteFile(fileTree)) {
			return Message.success();
		}
		return Message.error(FileMessageConstant.FILE_MESSAGE_FILE_DELETE_FAILED);
	}

	/**
	 * ????????????
	 */
	@Log(title = "????????????", action = "???????????????-????????????")
	@RequiresPermissions("spider:customspider:code:importFile")
	@ResponseBody
	@PostMapping("/uploadFile")
	public Message uploadFile(@RequestParam("file") MultipartFile file, HttpServletRequest request, FileTree fileTree) {
		// ?????????????????????
		if (OtherUtils.isTestManager(roleService.selectRoleKeys(ShiroUtils.getUserId()))) {
			return Message.error(UserConstants.USER_MESSAGE_TEATADMIN_DISABLED_USE);
		}
		// ??????id???childid???????????????????????????????????????
		FileTree fileTree2 = fileTreeService.selectFileTreeByCSIdAndChildId(fileTree);
		fileTree.setFilePath(fileTree2.getFilePath());
		fileTree.setCustomSpiderBackId(fileTree2.getCustomSpiderBackId());
		File file2 = new File(fileTree.getFilePath());
		if (!file2.exists()) {
			return Message.error(FileMessageConstant.FILE_MESSAGE_FILE_NO_EXIST);
		}
		// if (fileTree.getFlag() == 3 && file2.listFiles().length > 0) {
		// return
		// Message.error(FileMessageConstant.FILE_MESSAGE_FILE_EXIST_TO_DELETE);
		// }

		// ?????????
		String fileName = file.getOriginalFilename();

		// ?????????
		try {
			FileUploadUtils.uploadFile(file.getBytes(), fileTree.getFilePath(), fileName);
		} catch (Exception e) {
			return Message.error(FileMessageConstant.FILE_MESSAGE_FILE_UPLOAD_FAILED);
		}
		// if (fileTree.getFlag() == 3) {
		// //jar??????????????????????????????????????????
		//
		// }
		// ???????????????
		cSFileService.updateFileTreeToDB(cSFileService.selectCSFileTreeToList(fileTree2.getCustomSpiderId()),
				fileTree2.getCustomSpiderId(), fileTree2.getCustomSpiderBackId());
		return Message.success();
	}

	/**
	 * ????????????
	 */
	@Log(title = "????????????", action = "???????????????-????????????")
	@RequiresPermissions("spider:customspider:code:exportFile")
	@GetMapping("/exportFile/{customSpiderId}/{childId}")
	public void exportFile(HttpServletResponse response, @PathVariable("customSpiderId") Integer customSpiderId,
			@PathVariable("childId") Integer childId) throws IOException {
		response.reset();
		// ??????id???childid???????????????????????????????????????
		FileTree fileTree = new FileTree();
		fileTree.setCustomSpiderId(customSpiderId);
		fileTree.setChildId(childId);
		FileTree fileTree2 = fileTreeService.selectFileTreeByCSIdAndChildId(fileTree);
		fileTree.setFilePath(fileTree2.getFilePath());
		fileTree.setCustomSpiderBackId(fileTree2.getCustomSpiderBackId());
		File file2 = new File(fileTree.getFilePath());
		if (!file2.exists()) {
			return;
		}
		// ????????????, ?????????????????????????????????(????????????)
		List<String> list = new ArrayList<String>();
		list.add(fileTree.getFilePath());
		byte[] data = FileUtils.getAllFileToByte(list, true, null);
		String fileName = (!CommonSymbolicConstant.EMPTY_STRING.equals(basesetService.getDownloadFileNamePrefix())
				? (basesetService.getDownloadFileNamePrefix() + CommonSymbolicConstant.UNDERLINE)
				: CommonSymbolicConstant.EMPTY_STRING) + SpiderConstant.SPIDER_EXPORTFILE_DEFAULT_NAME;
		// ??????????????????
		try {
			fileName = FileUtils.getNewString(fileName);
		} catch (Exception e) {
			e.printStackTrace();
			fileName = SpiderConstant.SPIDER_EXPORTFILE_DEFAULT_NAME;
		}
		response.reset();
		response.setHeader(FileMessageConstant.FILE_CONTENT_DISPOSITION,
				FileMessageConstant.FILE_ATTACHMENT_FILENAME + fileName);
		response.addHeader(FileMessageConstant.FILE_CONTENT_LENGTH, CommonSymbolicConstant.EMPTY_STRING + data.length);
		response.setContentType(FileMessageConstant.FILE_CONTENT_TYPE);

		IOUtils.write(data, response.getOutputStream());
		response.getOutputStream().close();
	}

	/**
	 * ????????????
	 */
	@Log(title = "????????????", action = "???????????????-????????????")
	@RequiresPermissions("spider:customspider:code:exportFile")
	@GetMapping("/exportProject/{customSpiderId}")
	public void exportProject(HttpServletResponse response, @PathVariable("customSpiderId") Integer customSpiderId)
			throws IOException {
		response.reset();
		// ??????id???childid???????????????????????????????????????
		FileTree fileTree = new FileTree();
		fileTree.setCustomSpiderId(customSpiderId);
		// ???????????????,???????????????????????????
		Customspider customspider = customspiderService.selectCustomspiderById(customSpiderId);
		// ???????????????????????????????????????
		CodeType codeType = codeTypeMapper.selectSpiderCodeTypeByCustomSpiderType(customspider.getCustomSpiderType());
		String filePath1 = CommonSymbolicConstant.EMPTY_STRING;
		String filePath2 = CommonSymbolicConstant.EMPTY_STRING;
		List<String> list = new ArrayList<String>();
		// java????????????
		filePath1 = FilePathConfig.getCustomSpiderPath() + File.separator + codeType.getSpiderCodeTypeFolder()
				+ File.separator + customspider.getCustomSpiderBackId();
		if (customspider.getCustomSpiderType() == 0) {
			filePath2 = FilePathConfig.getCustomSpiderPath() + File.separator + codeType.getSpiderCodeTypeFolder()
					+ File.separator + SpiderConstant.SPIDER_JAVA_PUBLIC_DEPENDENCY_FOLDER;
		} else if (customspider.getCustomSpiderType() == 1) {
			// python
			filePath2 = FilePathConfig.getCustomSpiderPath() + File.separator + codeType.getSpiderCodeTypeFolder()
					+ File.separator + SpiderConstant.SPIDER_PYTHON_PUBLIC_DEPENDENCY_FOLDER;
		} else if (customspider.getCustomSpiderType() == 2) {
			// js
			filePath2 = FilePathConfig.getCustomSpiderPath() + File.separator + codeType.getSpiderCodeTypeFolder()
					+ File.separator + SpiderConstant.SPIDER_JAVASCRIPT_PUBLIC_DEPENDENCY_FOLDER;
		} else if (customspider.getCustomSpiderType() == 3) {
			// jar???
			filePath2 = FilePathConfig.getCustomSpiderPath() + File.separator + codeType.getSpiderCodeTypeFolder()
					+ File.separator + SpiderConstant.SPIDER_JAVA_PUBLIC_DEPENDENCY_FOLDER;
		}

		list.add(filePath1);
		list.add(filePath2);
		byte[] data = FileUtils.getAllFileToByte(list, true, String.valueOf(customspider.getCustomSpiderBackId()));
		String fileName = (!CommonSymbolicConstant.EMPTY_STRING.equals(basesetService.getDownloadFileNamePrefix())
				? (basesetService.getDownloadFileNamePrefix() + CommonSymbolicConstant.UNDERLINE)
				: CommonSymbolicConstant.EMPTY_STRING) + SpiderConstant.SPIDER_EXPORTPROJECT_DEFAULT_NAME;
		// ??????????????????
		try {
			fileName = FileUtils.getNewString(fileName);
		} catch (Exception e) {
			e.printStackTrace();
			fileName = SpiderConstant.SPIDER_EXPORTPROJECT_DEFAULT_NAME;
		}
		response.reset();
		response.setHeader(FileMessageConstant.FILE_CONTENT_DISPOSITION,
				FileMessageConstant.FILE_ATTACHMENT_FILENAME + fileName);
		response.addHeader(FileMessageConstant.FILE_CONTENT_LENGTH, CommonSymbolicConstant.EMPTY_STRING + data.length);
		response.setContentType(FileMessageConstant.FILE_CONTENT_TYPE);

		IOUtils.write(data, response.getOutputStream());
		response.getOutputStream().close();
	}

	// ??????????????????
	@Log(title = "????????????", action = "???????????????-??????????????????")
	@PostMapping("/setEntryFile")
	@ResponseBody
	public Message setEntryFile(FileTree fileTree) {
		// ?????????????????????
		if (OtherUtils.isTestManager(roleService.selectRoleKeys(ShiroUtils.getUserId()))) {
			return Message.error(UserConstants.USER_MESSAGE_TEATADMIN_DISABLED_USE);
		}
		// ????????????
		// ?????????????????????
		Customspider customspider = customspiderService
				.selectCustomspiderByCustomSpiderBackId(fileTree.getCustomSpiderBackId());
		Customspider customspider2 = new Customspider();
		customspider2.setCustomSpiderBackId(customspider.getCustomSpiderBackId());
		customspider2.setEntryFileName(fileTree.getEntryFileName());

		fileTree.setSpiderCodeTypeFolder(customspider.getSpiderCodeTypeFolder());
		if (customspider.getCustomSpiderType() == 0) {
			fileTree.setFileName(
					fileTree.getEntryFileName() + FileExtensionConstant.FILE_EXTENSION_POINT_CODEFILE_JAVA);
			// java??????
			// ???????????????????????????main??????
			if (!cSFileService.checkJavaFile(fileTree, (customspider.getSpiderJavaPackagePrefix() != null
					? customspider.getSpiderJavaPackagePrefix() : CommonSymbolicConstant.EMPTY_STRING))) {
				return Message.error(SpiderMessageConstant.SPIDER_MESSAGE_NO_MAIN_METHOD);
			}

		}
		// ???????????????
		if (customspiderService.updateCustomspiderEntryFileName(customspider2) > 0) {
			return Message.success();
		}
		return Message.error(FileMessageConstant.FILE_MESSAGE_FILE_UNKNOWN_MISTAKE);
	}

}
