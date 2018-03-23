package com.trs.util;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletResponse;


import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfWriter;
import com.ssh.util.WeblogicClassPath;
import com.ssh.util.CheckData;
/**
 * @author tan.hongyan
 * @version: Nov 25, 2009 10:08:06 AM
 * @desc: 
 */
public class PdfTool {

	/**
	 * 
	 * @param fileName
	 * @param response
	 * @param titleStr		""：没title
	 * @param titleFont		样式
	 * @param contentFont	样式
	 * @param addContent	Element数组，pdf内容
	 */
	public void writePdf(String fileName, HttpServletResponse response, String titleStr, Font titleFont, Font contentFont, Element[] addContent) {
		
		response.setHeader("Content-disposition","attachment; filename=" + CheckData.c(fileName));
		response.setHeader("Pragma", "public"); 
		response.setHeader("Cache-Control", "max-age=30" );
		response.setContentType("application/octet-stream charset=utf-8");
		
		Document document = new Document();
		
		try {
			PdfWriter.getInstance(document, response.getOutputStream());
			document.open();
			
			//title
			if(!"".equals(titleStr)) {
				Paragraph title = new Paragraph(titleStr + "\n\n", titleFont);
				title.setAlignment(Paragraph.ALIGN_CENTER);
				document.add(title);
			}
			
			//body
			for(Element content : addContent) {
				document.add(content);
			}

			//tail
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String now = sdf.format(new Date());
			
			Paragraph graph = new Paragraph("\n\n打印日期：" + now, contentFont);
			graph.setAlignment(Paragraph.ALIGN_RIGHT);
			graph.setIndentationRight(25f);
			document.add(graph);
			
			String path = WeblogicClassPath.getClassPath(PdfTool.class);
	        String temp = "WEB-INF/classes";
	        path = path.substring(0, path.length() - 1 - temp.length());
	        String imgPath = path + "/images/seal.gif";
			Image image = Image.getInstance(imgPath);
			image.setAlignment(Image.ALIGN_RIGHT);
			//image.setAbsolutePosition(440, 440);
			image.scaleToFit(105, 105);
			
			Phrase p = new Phrase();
			p.add(new Chunk(image, 382, - image.getHeight() / 7));
			document.add(p);
			
			String[] buttomStr = { "\n\n\n\n上投摩根基金管理有限公司\n",
					"China International Fund Management Co., Ltd.\n",
					"上海市浦东新区富城路99号震旦大厦20层(200120)\n",
					"20th Floor, AURORA Plaza, No.99 Fucheng Road, Pudong Shanghai, P.R.C., 200122\n"};
			
			for(String buttom : buttomStr) {
				graph = new Paragraph(buttom, contentFont);
				graph.setAlignment(Paragraph.ALIGN_CENTER);
				document.add(graph);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		document.close();
		
	}
      public void writePdf_en(String fileName, HttpServletResponse response, String titleStr, Font titleFont, Font contentFont, Element[] addContent) {
		
		response.setHeader("Content-disposition","attachment; filename=" + fileName);
		response.setHeader("Pragma", "public"); 
		response.setHeader("Cache-Control", "max-age=30" );
		response.setContentType("application/octet-stream charset=utf-8");
		
		Document document = new Document();
		
		try {
			PdfWriter.getInstance(document, response.getOutputStream());
			document.open();
			
			//title
			if(!"".equals(titleStr)) {
				Paragraph title = new Paragraph(titleStr + "\n\n", titleFont);
				title.setAlignment(Paragraph.ALIGN_CENTER);
				document.add(title);
			}
			
			//body
			for(Element content : addContent) {
				document.add(content);
			}

			//tail
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String now = sdf.format(new Date());
			
			Paragraph graph = new Paragraph("\n\nPrint Date：" + now, contentFont);
			graph.setAlignment(Paragraph.ALIGN_RIGHT);
			graph.setIndentationRight(25f);
			document.add(graph);
			
			String path = WeblogicClassPath.getClassPath(PdfTool.class);
	        String temp = "WEB-INF/classes";
	        path = path.substring(0, path.length() - 1 - temp.length());
	        String imgPath = path + "/images/seal.gif";
	        
			Image image = Image.getInstance(imgPath);
			image.setAlignment(Image.ALIGN_RIGHT);
			//image.setAbsolutePosition(440, 440);
			image.scaleToFit(105, 105);
			
			Phrase p = new Phrase();
			p.add(new Chunk(image, 382, - image.getHeight() / 7));
			document.add(p);
			
			String[] buttomStr = { "\n\n\n\n上投摩根基金管理有限公司\n",
					"China International Fund Management Co., Ltd.\n",
					"上海市浦东新区富城路99号震旦大厦20层(200120)\n",
					"20th Floor, AURORA Plaza, No.99 Fucheng Road, Pudong Shanghai, P.R.C., 200122\n"};
			
			for(String buttom : buttomStr) {
				graph = new Paragraph(buttom, contentFont);
				graph.setAlignment(Paragraph.ALIGN_CENTER);
				document.add(graph);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		document.close();
		
	}
}
