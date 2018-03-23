/**
 * 完美解决了乱码问题
 * 1、post 不会乱码
 * 2、get  也不回乱码
 */

package com.trs.filter;

/**
 * @author Tan hongyan E-mail:tan.hongyan@trs.com.cn
 * @version ����ʱ�䣺Dec 29, 2008 10:53:25 PM
 * ��˵��
 */
import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import com.ssh.util.CheckData;
public class EncodingFilter implements Filter {

	private String charset = null;

	private String defaultCharset = "iso-8859-1";

	public void destroy() {
		// TODO Auto-generated method stub

	}
	/**
	 * 重新构建HttpRequest
	 * @author admin
	 *
	 */
	class HttpRequest extends HttpServletRequestWrapper {

		public HttpRequest(HttpServletRequest request) {
			super(request);
		}

		private HttpServletRequest getHttpServletRequest() {
			return (HttpServletRequest) super.getRequest();
		}

		public String getParameter(String name) {
			String sTemp=CheckData.c(getHttpServletRequest().getParameter(name));			 
			if(sTemp==null) {
				return null;
			}
			try {
				sTemp=new String(sTemp.getBytes(defaultCharset), charset ); 
			}catch(Exception e) {
				
			}			 
			return sTemp;
		}

	} 

	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain chain) throws IOException, ServletException {
		// by sxl
		HttpServletRequest httpRequest = (HttpServletRequest) req;
		String sMethod = httpRequest.getMethod();
		
		if (sMethod.toLowerCase().equals("get")) {
			//重新构造一个Request
			req = new HttpRequest(httpRequest);
		}

		req.setCharacterEncoding(this.charset);
		chain.doFilter(req, resp);
	}

	public void init(FilterConfig arg0) throws ServletException {
		this.charset = arg0.getInitParameter("charset");
	}

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public String getDefaultCharset() {
		return defaultCharset;
	}

	public void setDefaultCharset(String defaultCharset) {
		this.defaultCharset = defaultCharset;
	}

}
