package com.softigent.sftselenium;

import java.io.File;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.MethodSorters;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

@FixMethodOrder(MethodSorters.JVM)
public abstract class BaseTest {
	
	protected Connector connector;

	protected Logger log;
	protected String className;

	@Rule
	public TestWatcher testWatchThis = new TestWatcher() {
		@Override
		protected void failed(Throwable e, Description description) {
			doFailed(e, description);
		}
	};
	
	public final Container body;

	public BaseTest(Connector connector) {
		if (connector == null) {
			throw new NullPointerException("Please initialize a default connector.");
		}
		this.connector = connector;
		this.className = getClass().getName();
		this.body = createContainer("body");
		
		String logger_stack = connector.getConfig().getProperty("logger_stack_size");
		if (logger_stack != null && !Double.isNaN(Double.valueOf(logger_stack))) {
			CacheLogger.MAX_STACK_SIZE = Integer.parseInt(logger_stack);
		}
		initialize();
	}

	protected String doFailed(Throwable e, Description description) {
		return String.join("\n", CacheLogger.getLastMessages());
	}
	
	protected void initialize() {
		log = CacheLogger.getLogger(className);
	}
	
	public static Connector createConnector(Config config, String driverName) {
		String driverPath = config.getProperty("chrome_driver_path");
		if (driverPath != null) {
			System.setProperty("webdriver.chrome.driver", new File(driverPath).getAbsolutePath());
		}
		
		driverPath = config.getProperty("ie_driver_path");
		if (driverPath != null) {
			System.setProperty("webdriver.ie.driver", new File(driverPath).getAbsolutePath());
		}

		WebDriver driver = SeleniumUtils.getDriver(driverName, config);
		return new Connector(driver, config);
	}
	
	public Container createContainer(String selector) {
		return new Container(connector.getDriver(), connector.getConfig(), selector);
	}
	
	public Container createContainer(WebElement element) {
		return createContainer(element, createContainer("body"));
	}
	
	public Container createContainer(WebElement element, Container container) {
		return new Container(connector.getDriver(), connector.getConfig(), element);
	}
	
	public Container createContainer(WebElement element, String xpath) {
		return new Container(connector.getDriver(), connector.getConfig(), Container.getParent(element, xpath));
	}
	
	public Container createWindowContainer(String windowTitle) {
		return new Container(switchWindow(windowTitle), connector.getConfig(), "body");
	}
	
	public void waitPageLoad() {
		SeleniumUtils.wait(connector.getDriver(), connector.getConfig().getPageLoadTimeout(), null);
	}

	public void waitPageLoad(String urlPath) {
		SeleniumUtils.wait(connector.getDriver(), connector.getConfig().getPageLoadTimeout(), Pattern.compile(urlPath));
	}

	/*
	 * script - FakeDate.prototype.__proto__.getTimezoneOffset = function() { return 0; }
	 */
	public Object execFakeDate(String script) {
		return executeScript("function FakeDate() {" +
			"var args = Array.prototype.slice.call(arguments); " +
			"args.unshift(null); return new (Function.prototype.bind.apply(FakeDate.originalDate, args));} " +
			"FakeDate.prototype.__proto__ = Date.prototype; FakeDate.originalDate = Date; Date = FakeDate; " + 
			script);
	}

	public Object executeScript(String command) {
		return this.executeScript(command, null);
	}

	public Object executeScript(String command, Object element) {
		JavascriptExecutor js = (JavascriptExecutor) body.getDriver();
		if (element == null) {
			return js.executeScript(command);
		} else {
			return js.executeScript(command, element);
		}
	}
	
	public String getCurrentURL() {
		return connector.getDriver().getCurrentUrl();
	}
	
	public void openPage(String url) {
		log.info("Opening " + url);
		connector.getDriver().get(url);
	}
	
	public void gotoURL(String path) {
		String url = getCurrentURL();
		String host = url.substring(0,  url.indexOf('/', url.indexOf("//") + 2));
		log.info("To URL " + host + path);
		connector.getDriver().navigate().to(host + path);
	}
	
	public void gotoPage(String pageName) {
		String url = getCurrentURL();
		String uri = url.substring(0,  url.lastIndexOf('/'));
		log.info("To Page " + uri + pageName);
		connector.getDriver().navigate().to(uri + pageName);
	}
	
	public void closeBrowser() {
		connector.getDriver().quit();
	}
	
	public void closeOtherTabs() {
		String currentTab = connector.getDriver().getWindowHandle();
		for(String winHandle : connector.getDriver().getWindowHandles()) {
			if (!winHandle.equals(currentTab)) {
				connector.getDriver().switchTo().window(winHandle).close();
			}
		}
		connector.getDriver().switchTo().window(currentTab);
	}
	
	public Set<String> getWindowHandles() {
		return connector.getDriver().getWindowHandles();
	}
	
	public String getWindowHandle() {
		return connector.getDriver().getWindowHandle();
	}
	
	public String getWindowHandleByURL(String regex) {
		WebDriver driver = connector.getDriver();
		Set<String> windows = driver.getWindowHandles();
	
		for (String window : windows) {
			try {
				driver.switchTo().window(window);
				Boolean isTrue = Container.compareString(regex, driver.getCurrentUrl());
				if (isTrue) {
					return window;
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public WebDriver switchWindow(String winHandle) {
		return connector.getDriver().switchTo().window(winHandle);
	}

	public static void print(Object message) {
		System.out.println(message);
	}
	
	@Rule
	public TestWatcher testWatcher = new TestWatcher() {
		@Override
		protected void starting(final Description description) {
			log.info(">>>>> Start " + description.getClassName() + "::" + description.getMethodName());
		}

		@Override
		protected void finished(final Description description) {
			log.info(">>>>> End " + description.getClassName() + "::" + description.getMethodName());
		}
	};
}