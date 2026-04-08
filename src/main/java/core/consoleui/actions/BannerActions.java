package core.consoleui.actions;

import lib.compat.FluentWebElement;
import org.testng.Assert;

public class BannerActions extends CommercePageActions {

    public void goToFieldRuleBanner()
    {
        awaitForElementPresence(bannerFieldRuleButton);
        click(bannerFieldRuleButton);
    }

    public void goToQueryRuleBanner()
    {
            if (awaitForElementPresence(bannerQueryRuleButton)) {
                safeClick(bannerQueryRuleButton);
            }
    }

    public void fillHtmlBanner(String html)
    {
        awaitForElementPresence(htmlBannerInput);
        threadWait();
        Assert.assertTrue(htmlRadioButtonIsSelected.isSelected(),"Html Radio Button is not selected");
        htmlBannerInput.clear();
        threadWait();
        htmlBannerInput.fill().with(html);
    }

    public void addHtmlBanner(String html)
    {
        click(htmlRadioButton);
        threadWait();
        fillHtmlBanner(html);
        threadWait();
        findFirst("body").click();
    }

    public void addImgBanner(String ImgUrl)
    {
        awaitForElementPresence(imageUrlRadioButton);
        threadWait();
        click(imageUrlRadioButton);
        awaitForElementPresence(bannerInputImgUrl);
        Assert.assertTrue(imgUrlRadioButtonIsSelected.isSelected(),"ImageUrl Radio Button is not selected");
        bannerInputImgUrl.clear();
        threadWait();
        bannerInputImgUrl.fill().with(ImgUrl);
        bannerInputRedirectUrl.clear();
        bannerInputRedirectUrl.fill().with(ImgUrl);
        ThreadWait();
        findFirst("body").click();
    }


    public void fillRedirectURL(String redirctUrl)
    {
        awaitForElementPresence(redirectInput);
        ThreadWait();
        redirectInput.clear();
        ThreadWait();
        redirectInput.fill().with(redirctUrl);
        findFirst("body").click();
    }

    /** Selects the first option (index 0) in the field-rule attribute list and returns its visible label. */
    public String selectFieldRuleAttribute() throws InterruptedException {
        int optionIndex = 0;
        click(fieldRuleAttributeDropdown);
        threadWait();
        click(searchAttribute);
        threadWait();
        if (fieldRuleAttributeDropDownList.isEmpty()) {
            Assert.fail("FIELD RULE ATTRIBUTE LISTS ARE NOT DISPLAYED!!! PLEASE SELECT THE ATTRIBUTE FROM THE SETTINGS");
        }
        if (fieldRuleAttributeDropDownList.size() > optionIndex) {
            threadWait();
            FluentWebElement option = fieldRuleAttributeDropDownList.get(optionIndex);
            String attributeName = option.getText().trim();
            option.click();
            return attributeName;
        }
        Assert.fail("FIELD RULE ATTRIBUTE LIST HAS NO ITEM AT INDEX " + optionIndex
            + " (size=" + fieldRuleAttributeDropDownList.size() + ")");
        throw new IllegalStateException("unreachable");
    }

    /** Selects the first option (index 0) in the field-rule attribute value list. */
    public void selectFieldRuleAttributeValue() throws InterruptedException {
        int optionIndex = 0;
        click(fieldRuleAttributeValueDropdown);
        ThreadWait();
        if (fieldRuleAttributeDropDownList.isEmpty()) {
            Assert.fail("FIELD RULE ATTRIBUTE VALUES LISTS ARE NOT DISPLAYED!!!");
        }
        if (fieldRuleAttributeDropDownList.size() > optionIndex) {
            ThreadWait();
            fieldRuleAttributeDropDownList.get(optionIndex).click();
        } else {
            Assert.fail("FIELD RULE ATTRIBUTE VALUE LIST HAS NO ITEM AT INDEX " + optionIndex
                + " (size=" + fieldRuleAttributeDropDownList.size() + ")");
        }
    }

    /**
     * Scrolls to the bannerExperienceInput element to ensure it is in view.
     */
    public void scrollToBannerExperienceInput() {
        scrollToElement(bannerExperienceInput, "Banner Experience Input");
    }

}

