package UnbxdTests.testNG.ui.SearchManageTest;

import UnbxdTests.testNG.ui.BaseTest;
import core.ui.actions.LoginActions;
import lib.Helper;
import lib.constants.UnbxdErrorConstants;

import lib.compat.Page;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.Assert;
import lib.compat.FluentWebElement;

import core.consoleui.actions.ContentActions;
import core.consoleui.actions.MerasurementSearchAction;
import lib.EnvironmentConfig;

public class MerasurementSearchTest extends BaseTest {

    @Page
    MerasurementSearchAction merasurementSearchAction;

    @Page
    ContentActions contentActions;

    @Page
    LoginActions loginActions;

    @BeforeClass
    public void setUp() {
        super.setUp();
        EnvironmentConfig.unSetContext();
        EnvironmentConfig.setContext(2, 2);
    }

    @Test(priority = 1, description = "This Test configures and verifies measurement search", groups = {"measurement-search"})
    public void configureMeasurementSearchTest() throws InterruptedException 
    {
        goTo(merasurementSearchAction);

        merasurementSearchAction.enableMeasurementSearchIfDisabled();

        merasurementSearchAction.deleteExistingConfigAttribute();

        merasurementSearchAction.selectConfigAttributeFromDropDown();

        merasurementSearchAction.selectDimensionsFromDropDown();

        merasurementSearchAction.selectUnitFromDropDown();

        merasurementSearchAction.saveChanges();

        Assert.assertTrue(contentActions.checkSuccessMessage(), UnbxdErrorConstants.SUCCESS_MESSAGE_FAILURE);
    }

    @AfterClass
    public void tearDown() {
        if (driver != null) {
            driver.close();
            driver.quit();
            Helper.tearDown();
        }
    }
} 