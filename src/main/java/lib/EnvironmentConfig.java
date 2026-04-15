package lib;

import lib.config.Configuration;
import lib.config.Context;
import lib.config.Site;
import lib.config.UnbxdService;
import org.testng.Assert;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.LoaderOptions;

import java.io.InputStream;

public class EnvironmentConfig {

    /** Numeric Unbxd site id as in YAML {@code siteId: 139} — resolves to that row's yaml {@code id}. */
    public static final String PROP_CONSOLE_SITE_ID = "unbxd.console.site.id";
    /** Yaml site row id (field {@code id: 2}) used by {@link Configuration#getSiteById(int)}. */
    public static final String PROP_SITE_CONTEXT_ID = "unbxd.site.context.id";
    /** User row id in YAML {@code users:} (e.g. 1 or 2). */
    public static final String PROP_USER_ID = "unbxd.user.id";

    public  static String configFile;
    private static Configuration configuration;
    private static final String SITE_CONFIG_FILE="siteConfig.yml";

    private static ThreadLocal<Context> context=new ThreadLocal<Context>();

    public static void loadConfig() throws Exception {
        if(configuration==null)
        {
            LoaderOptions loaderOptions = new LoaderOptions();
            loaderOptions.setAllowDuplicateKeys(true);
            Yaml yaml = new Yaml(new Constructor(Configuration.class, loaderOptions));
            String resolvedProfile = System.getProperty("env.profile");
            if (resolvedProfile == null || resolvedProfile.trim().isEmpty()) {
                resolvedProfile = Config.getEnvironmentProfile();
            }
            if (resolvedProfile == null || resolvedProfile.trim().isEmpty() || resolvedProfile.contains("${")) {
                resolvedProfile = "ProdUK";
                System.out.println("No env.profile set, defaulting to: " + resolvedProfile);
            }
            String file= resolvedProfile + ".yaml";
            try(InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(file)) {
                if (in == null) {
                    throw new Exception("Yaml file not found on classpath: " + file);
                }
                configuration = yaml.loadAs(in, Configuration.class);
                configuration.setLoginUrl(configuration.getApplicationUrl());
            } catch(Exception ex) {
                ex.printStackTrace();
                throw new Exception(" Exception while loading the Yaml File "+file);
            }
        }
    }

    public static String getApplicationUrl()
    {
        return configuration.getApplicationUrl();
    }

    public static String getBaseUrl()
    {
        return configuration.getBaseUrl();
    }

    public static void setApplicationUrl(String url) {

        configuration.setApplicationUrl(url);
    }

    public static String getSiteKey()
    {
        int siteId=context.get().getSiteId();
        return configuration.getSiteById(siteId).getSiteKey();
    }

    public static String getStatusById()
    {
        int siteId=context.get().getSiteId();
        return configuration.getSiteById(siteId).getStatusById();
    }

    public static String getRegion()
    {
        int siteId = context.get().getSiteId();
        return configuration.getSiteById(siteId).getRegion();
    }

    public static String getApiKey()
    {
        int siteId=context.get().getSiteId();
        return configuration.getSiteById(siteId).getApiKey();

    }

    public static String getSecreteKey()
    {
        int siteId=context.get().getSiteId();
        return configuration.getSiteById(siteId).getSecretKey();
    }

    public static String getSiteName()
    {
        int siteId=context.get().getSiteId();

        return configuration.getSiteById(siteId).getName();
    }

    public static String getEmail()
    {
        int userId=context.get().getUserId();
        return configuration.getUserById(userId).getEmail();
    }

    public static String getDeleteCookie()
    {
        int userId=context.get().getUserId();
        return configuration.getUserById(userId).getCookie();
    }

    public static String getServiceUrl(String service){

        for(UnbxdService unbxdService:configuration.getServices()){

            if(unbxdService.getName().equalsIgnoreCase(service)){
                return unbxdService.getHost();
            }
        }
        return "";
    }

    public static String getSiteId()
    {
        int siteId=context.get().getSiteId();
        return String.valueOf(configuration.getSiteById(siteId).getSiteId());
    }

    public static String getPassword()
    {
        int userId=context.get().getUserId();
        return configuration.getUserById(userId).getPassword();
    }

    public static String getLoginUrl(){
        return configuration.getLoginUrl();
    }

    // public static void setContext(int userId, int siteId){

    //     System.out.println("Setting the Context UserId:"+userId+ " , siteId : "+siteId);
    //     if(context.get()!=null)
    //         Assert.fail("Context is already Set , cant recreate it");
    //     Context newContext=getNewContext(userId,siteId);
    //     context.set(newContext);
    // }

    public static void setContext(int userId, int siteId) {
    if (context.get() != null) {
        System.out.println("Context already set. Skipping reinitialization.");
        return;
    }

    System.out.println("Setting the Context UserId:" + userId + " , siteId : " + siteId);
    Context newContext = getNewContext(userId, siteId);
    context.set(newContext);
}

    public static void unSetContext(){
        if (context.get() != null) {
        System.out.print("Removing the Context userId: "+context.get().getUserId()+" , SiteId: "+context.get().getSiteId());
        context.remove();
        } else {
            System.out.println("No context to remove - context is already null");
        }
    }

    private static Context getNewContext(int userId, int siteId){
        Context context=new Context();
        context.setUserId(userId);
        context.setSiteId(siteId);
        return context;
    }

    /**
     * Resolves which YAML site row to use for login ({@link Context#setSiteId}).
     * <ol>
     *   <li>If {@value #PROP_CONSOLE_SITE_ID} is set (e.g. 139), finds the site with that {@code siteId}.</li>
     *   <li>Else if {@value #PROP_SITE_CONTEXT_ID} is set, uses it as yaml {@code id}.</li>
     *   <li>Else {@code defaultSiteContextId} (e.g. 2 for Dev collections).</li>
     * </ol>
     */
    public static int resolveLoginSiteContextId(int defaultSiteContextId) {
        try {
            loadConfig();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load environment config for site resolution", e);
        }
        String consoleProp = System.getProperty(PROP_CONSOLE_SITE_ID);
        if (consoleProp != null && !consoleProp.trim().isEmpty()) {
            long num = Long.parseLong(consoleProp.trim());
            Site found = configuration.findSiteByConsoleSiteId(num);
            if (found == null) {
                throw new IllegalStateException(
                        "No site in environment YAML with " + PROP_CONSOLE_SITE_ID + "=" + num
                                + ". Check env.profile and sites[].siteId values.");
            }
            System.out.println(
                    "Run config: " + PROP_CONSOLE_SITE_ID + "=" + num + " -> yaml site id=" + found.getId());
            return found.getId();
        }
        String ctxProp = System.getProperty(PROP_SITE_CONTEXT_ID);
        if (ctxProp != null && !ctxProp.trim().isEmpty()) {
            int v = Integer.parseInt(ctxProp.trim());
            System.out.println("Run config: " + PROP_SITE_CONTEXT_ID + "=" + v);
            return v;
        }
        System.out.println("Run config: using default yaml site id=" + defaultSiteContextId);
        return defaultSiteContextId;
    }

    /**
     * User id from {@value #PROP_USER_ID}, or {@code defaultUserId}.
     */
    public static int resolveUserId(int defaultUserId) {
        String u = System.getProperty(PROP_USER_ID);
        if (u != null && !u.trim().isEmpty()) {
            int v = Integer.parseInt(u.trim());
            System.out.println("Run config: " + PROP_USER_ID + "=" + v);
            return v;
        }
        return defaultUserId;
    }

}
