package com.rzspider.project.book.intention.controller;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.tomcat.util.http.fileupload.FileUploadBase.FileSizeLimitExceededException;
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

import com.rzspider.project.book.bookmanage.domain.Bookmanage;
import com.rzspider.project.book.intention.domain.Intention;
import com.rzspider.project.book.intention.service.IIntentionService;
import com.rzspider.project.book.intention.utils.IntentionUtils;
import com.rzspider.project.common.file.utilt.FileUtils;
import com.rzspider.project.monitor.operlog.domain.OperLog;
import com.rzspider.project.system.role.service.IRoleService;
import com.rzspider.project.tool.baseset.service.IBasesetService;
import com.rzspider.common.constant.CommonSymbolicConstant;
import com.rzspider.common.constant.FileExtensionConstant;
import com.rzspider.common.constant.FileMessageConstant;
import com.rzspider.common.constant.project.BookConstant;
import com.rzspider.common.utils.FileUploadUtils;
import com.rzspider.common.utils.OtherUtils;
import com.rzspider.common.utils.security.ShiroUtils;
import com.rzspider.framework.aspectj.lang.annotation.Log;
import com.rzspider.framework.config.FilePathConfig;
import com.rzspider.framework.web.controller.BaseController;
import com.rzspider.framework.web.page.TableDataInfo;
import com.rzspider.framework.web.domain.Message;

/**
 * ???????????? ??????????????????
 * 
 * @author ricozhou
 * @date 2018-05-28
 */
@Controller
@RequestMapping("/book/intention")
public class IntentionController extends BaseController {
	private String prefix = "book/intention";
	public List<Intention> itList;
	@Autowired
	private IIntentionService intentionService;
	@Autowired
	private FilePathConfig filePathConfig;
	@Autowired
	private IBasesetService basesetService;

	@Log(title = "????????????", action = "??????????????????-????????????")
	@GetMapping()
	@RequiresPermissions("book:intention:view")
	public String intention() {
		return prefix + "/intention";
	}

	/**
	 * ????????????????????????
	 */
	@RequiresPermissions("book:intention:list")
	@GetMapping("/list")
	@ResponseBody
	public TableDataInfo list(Intention intention) {
		startPage();
		List<Intention> list = intentionService.selectIntentionList(intention);
		return getDataTable(list);
	}

	/**
	 * ??????????????????
	 */
	@Log(title = "????????????", action = "??????????????????-????????????")
	@RequiresPermissions("book:intention:add")
	@GetMapping("/add")
	public String add() {
		return prefix + "/add";
	}

	/**
	 * ??????????????????
	 */
	@Log(title = "????????????", action = "??????????????????-????????????")
	@RequiresPermissions("book:intention:batchAdd")
	@GetMapping("/batchAdd")
	public String batchAdd() {
		return prefix + "/batchAdd";
	}

	/**
	 * ????????????Excel???????????????????????????
	 */
	@Log(title = "????????????", action = "??????????????????-????????????")
	@RequiresPermissions("book:intention:batchAnalyzeList")
	@ResponseBody
	@PostMapping("/batchAnalyzeList")
	public Message upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
		// ?????????????????????????????????
		try {
			FileUploadUtils.assertAllowed(file);
		} catch (FileSizeLimitExceededException e1) {
			e1.printStackTrace();
			itList.clear();
			return Message.error();
		}
		// ????????????
		String fileName = file.getOriginalFilename();
		if (!fileName.toLowerCase().endsWith(FileExtensionConstant.FILE_EXTENSION_POINT_EXCEL_XLS)
				&& !fileName.toLowerCase().endsWith(FileExtensionConstant.FILE_EXTENSION_POINT_EXCEL_XLSX)) {
			itList.clear();
			return Message.error(FileMessageConstant.FILE_MESSAGE_SIZE_GREATER_TEN_M);
		}
		// ?????????
		fileName = FileUploadUtils.renameToUUID(fileName);
		// ?????????
		try {
			FileUploadUtils.uploadFile(file.getBytes(), filePathConfig.getUploadCachePath(), fileName);
		} catch (Exception e) {
			itList.clear();
			return Message.error();
		}
		// ?????????????????????
		itList = intentionService.batchAnalyzeList(filePathConfig.getUploadCachePath(), fileName);
		if (itList == null || itList.size() < 1) {
			return Message.error(BookConstant.BOOK_MESSAGE_NO_DATA);
		}
		return Message.success();
	}

	@Log(title = "????????????", action = "??????????????????-????????????2")
	@ResponseBody
	@GetMapping("/batchAnalyzeList2")
	public TableDataInfo batchAnalyzeList2() {
		TableDataInfo tdi = getDataTable(itList);
		return tdi;
	}

	/**
	 * ????????????????????????
	 */
	@Log(title = "????????????", action = "??????????????????-????????????")
	@RequiresPermissions("book:intention:batchSave")
	@Transactional(rollbackFor = Exception.class)
	@PostMapping("/batchSave")
	@ResponseBody
	public Message batchSave() {
		if (itList == null || itList.size() < 1) {
			itList.clear();
			return Message.error();
		}
		if (intentionService.batchSaveIntention(itList) > 0) {
			itList.clear();
			return Message.success();
		}
		itList.clear();
		return Message.error();
	}

	/**
	 * ????????????
	 */
	@RequiresPermissions("book:intention:downExcelTemplate")
	@Log(title = "????????????", action = "??????????????????-????????????")
	@GetMapping("/downExcelTemplate")
	public void downExcelTemplate(HttpServletResponse response) throws IOException {
		byte[] data = intentionService.downExcelTemplate();
		response.reset();
		String excelName = IntentionUtils.getExcelTemplateFileName(basesetService.getDownloadFileNamePrefix());
		// ??????????????????
		try {
			excelName = FileUtils.getNewString(excelName);
		} catch (Exception e) {
			e.printStackTrace();
			excelName = BookConstant.BOOK_INTENTION_DEFAULT_EXCELTEMPLATE_NAME;
		}
		response.setHeader(FileMessageConstant.FILE_CONTENT_DISPOSITION,
				FileMessageConstant.FILE_ATTACHMENT_FILENAME + excelName);
		response.addHeader(FileMessageConstant.FILE_CONTENT_LENGTH, CommonSymbolicConstant.EMPTY_STRING + data.length);
		response.setContentType(FileMessageConstant.FILE_CONTENT_TYPE);

		IOUtils.write(data, response.getOutputStream());
	}

	/**
	 * ????????????excel
	 */
	@RequiresPermissions("book:intention:batchExport")
	@Log(title = "????????????", action = "??????????????????-????????????excel")
	@GetMapping("/batchExport")
	public void batchExport(HttpServletResponse response, Intention intention) {
		try {
			response.reset();
			intention.setUserId(Integer.valueOf(String.valueOf(ShiroUtils.getUserId())));
			byte[] data = intentionService.batchExport(intention);
			if (data == null) {
				return;
			}
			String excelName = IntentionUtils.getBookExportFileName(basesetService.getDownloadFileNamePrefix());
			// ??????????????????
			try {
				excelName = FileUtils.getNewString(excelName);
			} catch (Exception e) {
				e.printStackTrace();
				excelName = BookConstant.BOOK_INTENTION_DEFAULT_BOOKEXPORT_NAME;
			}
			response.setHeader(FileMessageConstant.FILE_CONTENT_DISPOSITION,
					FileMessageConstant.FILE_ATTACHMENT_FILENAME + excelName);
			response.addHeader(FileMessageConstant.FILE_CONTENT_LENGTH,
					CommonSymbolicConstant.EMPTY_STRING + data.length);
			response.setContentType(FileMessageConstant.FILE_CONTENT_TYPE);
			IOUtils.write(data, response.getOutputStream());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * ??????????????????
	 */
	@Log(title = "????????????", action = "??????????????????-????????????")
	@RequiresPermissions("book:intention:edit")
	@GetMapping("/edit/{bookId}")
	public String edit(@PathVariable("bookId") Integer bookId, Model model) {
		Intention intention = intentionService.selectIntentionById(bookId);
		model.addAttribute("intention", intention);
		return prefix + "/edit";
	}

	/**
	 * ??????????????????
	 */
	@Log(title = "????????????", action = "??????????????????-????????????")
	@RequiresPermissions("book:intention:save")
	@PostMapping("/save")
	@ResponseBody
	public Message save(Intention intention) {
		if (intentionService.saveIntention(intention) > 0) {
			return Message.success();
		}
		return Message.error();
	}

	/**
	 * ??????????????????
	 */
	@Log(title = "????????????", action = "??????????????????-????????????")
	@RequiresPermissions("book:intention:remove")
	@PostMapping("/remove/{bookId}")
	@ResponseBody
	public Message remove(@PathVariable("bookId") Integer bookId) {
		if (intentionService.deleteIntentionById(bookId) > 0) {
			return Message.success();
		}
		return Message.error();
	}

	/**
	 * ????????????????????????
	 */
	@Log(title = "????????????", action = "??????????????????-????????????")
	@RequiresPermissions("book:intention:batchRemove")
	@PostMapping("/batchRemove")
	@ResponseBody
	public Message remove(@RequestParam("ids[]") Integer[] bookIds) {
		int rows = intentionService.batchDeleteIntention(bookIds);
		if (rows > 0) {
			return Message.success();
		}
		return Message.error();
	}

	// ??????
	@Log(title = "????????????", action = "??????????????????-????????????")
	@RequiresPermissions("book:intention:detail")
	@GetMapping("/detail/{bookId}")
	public String detail(@PathVariable("bookId") Integer bookId, Model model) {
		Intention intention = intentionService.selectIntentionById(bookId);
		model.addAttribute("intention", intention);
		return prefix + "/detail";
	}
}
