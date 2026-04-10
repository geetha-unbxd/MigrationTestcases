package core.ui.actions;

import core.ui.page.WelcomePage;
import lib.ConsoleCredentialsHelper;
import lib.ConsoleGoogleLoginHelper;
import lib.EnvironmentConfig;
import lib.UrlMapper;
import org.openqa.selenium.Keys;
import org.testng.Assert;

public class LoginActions extends WelcomePage {

    public void login()
    {
        // Default login Uses SiteId 0 and UserId 1
        login(0,1);

    }

    public void login(int siteId,int userId)
    {
        EnvironmentConfig.setContext(userId,siteId);
        String email, password;
        email= EnvironmentConfig.getEmail();
        password=EnvironmentConfig.getPassword();
        loginWith(email,password);
    }

    public void unbxdLogin(int siteId,int userId)
    {
        EnvironmentConfig.setContext(userId,siteId);
        if (ConsoleCredentialsHelper.isConsoleGoogleLoginEnabled()) {
            performGoogleLoginOrFail();
            return;
        }
        goTo("https://console.unbxd.io/unbxdlogin");
        click(unbxdLoginButton);
        String email = EnvironmentConfig.getEmail();
        String password = EnvironmentConfig.getPassword();
        loginWithUnbxd(email,password);

    }

    @Override
    public String getUrl(){
//        return UrlMapper.LOGIN.getUrlPath();
        return UrlMapper.LOGIN.getUrlPathFromAppUrl(EnvironmentConfig.getLoginUrl());
    }

    private void loginWith(String email,String pwd) {
        if (ConsoleCredentialsHelper.isConsoleGoogleLoginEnabled()) {
            performGoogleLoginOrFail();
            return;
        }

        goTo(this);
        awaitForPageToLoadQuick();
        Assert.assertTrue(awaitForElementPresence(loginTitle), "Login page is not yet loaded");
        awaitForElementPresence(emailInputBox);
        click(emailInputBox);
        emailInputBox.clear();
        emailInputBox.fill().with(email);
        ThreadWait();
        awaitForElementPresence(passwordInputBox);
        click(passwordInputBox);
        passwordInputBox.clear();
        passwordInputBox.fill().with(pwd);
        ThreadWait();
        click(signIn);
        awaitForPageToLoad();
        ThreadWait();
        String currentUrl = getDriver().getCurrentUrl();
        if (currentUrl != null && currentUrl.contains("/login")) {
            System.out.println("Still on login page after sign-in attempt, login may have failed");
            if (awaitForElementPresence(getErrorMessageInLogin)==true) {
                System.out.println("Login is not working!!! The reason is " + getErrorMessageInLogin.getText());
            }
            Assert.fail("Login failed - still on login page: " + currentUrl);
        }
    }

    /**
     * When {@link ConsoleCredentialsHelper#isConsoleGoogleLoginEnabled()} is true, login is Google-only
     * (Node + cookies). There is no fallback to email/password forms.
     */
    private void performGoogleLoginOrFail() {
        if (!ConsoleCredentialsHelper.hasGoogleCredentials()) {
            Assert.fail("Console Google login is enabled but GOOGLE_EMAIL / GOOGLE_PASSWORD are missing. "
                    + "Set them in console-login/.env, or disable Google login with USE_CONSOLE_GOOGLE_LOGIN=false "
                    + "(-Duse.console.google.login=false) to use YAML form login.");
        }
        String targetUrl = EnvironmentConfig.getApplicationUrl();
        if (targetUrl == null || targetUrl.isBlank()) {
            targetUrl = EnvironmentConfig.getLoginUrl();
        }
        try {
            boolean ok = ConsoleGoogleLoginHelper.loginAndInjectCookies(getDriver(), targetUrl);
            Assert.assertTrue(ok, "Google login did not establish a session (still on login URL).");
        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            Assert.fail("Google login failed: " + e.getMessage(), e);
        }
    }

    private void loginWithUnbxd(String email,String pwd) {
        awaitForElementPresence(unbxdLoginButton);
        click(unbxdLoginButton);
        unbxdEmailInputBox.clear();
        unbxdEmailInputBox.getElement().sendKeys(email);
        unbxdEmailInputBox.getElement().sendKeys(Keys.ENTER);
        awaitForElementPresence(unbxdPasswordInputBox);
        click(unbxdPasswordInputBox);
        unbxdPasswordInputBox.clear();
        unbxdPasswordInputBox.fill().with(pwd);
        unbxdPasswordInputBox.getElement().sendKeys(Keys.ENTER);
        threadWait();
        awaitForPageToLoad();
        if (awaitForElementPresence(unbxdEmailInputBox)==false) {
            System.out.println("UNBXD Login is working");
        } else {
            Assert.fail("UNBXD Login is not working properly");
        }
    }

}
