package UnbxdTests.testNG.consoleui.AlgorithmTest;

import lib.compat.Page;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import core.consoleui.actions.ConceptsActions;
import core.consoleui.page.ConceptsPage;
import UnbxdTests.testNG.ui.BaseTest;
import org.testng.Assert;
import lib.constants.UnbxdErrorConstants;
import lib.Helper;
import core.ui.actions.LoginActions;
import core.consoleui.actions.StopWordAction;
import lib.EnvironmentConfig;

public class StopWordsTest extends BaseTest {

    @Page
    LoginActions loginActions;

    @Page
    ConceptsActions conceptsActions;

    @Page
    StopWordAction stopWordAction;

    @BeforeClass
    public void setUp() {
        super.setUp();
        EnvironmentConfig.unSetContext();
        EnvironmentConfig.setContext(2, 2);
    }

    @Test
    public void createStopWord() throws InterruptedException{

        goTo(stopWordAction);

        String keyword = conceptsActions.createKeyword();

        Assert.assertTrue(conceptsActions.checkSuccessMessage(), UnbxdErrorConstants.SUCCESS_MESSAGE_FAILURE);

        conceptsActions.deleteKeyword(keyword);

        Assert.assertTrue(conceptsActions.checkSuccessMessage(), UnbxdErrorConstants.SUCCESS_MESSAGE_FAILURE);

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

