package fr.free.nrw.commons.di;

import android.content.ContentProviderClient;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.util.LruCache;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.auth.AccountUtil;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.caching.CacheController;
import fr.free.nrw.commons.data.DBOpenHelper;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.mwapi.ApacheHttpClientMediaWikiApi;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.nearby.NearbyPlaces;
import fr.free.nrw.commons.upload.UploadController;

import static android.content.Context.MODE_PRIVATE;
import static fr.free.nrw.commons.contributions.ContributionsContentProvider.CONTRIBUTION_AUTHORITY;
import static fr.free.nrw.commons.modifications.ModificationsContentProvider.MODIFICATIONS_AUTHORITY;

@Module
@SuppressWarnings({"WeakerAccess", "unused"})
public class CommonsApplicationModule {
    public static final String CATEGORY_AUTHORITY = "fr.free.nrw.commons.categories.contentprovider";

    private CommonsApplication application;

    public CommonsApplicationModule(CommonsApplication application) {
        this.application = application;
    }

    @Provides
    public AccountUtil providesAccountUtil() {
        return new AccountUtil(application);
    }

    @Provides
    @Named("category")
    public ContentProviderClient provideCategoryContentProviderClient() {
        return application.getContentResolver().acquireContentProviderClient(CATEGORY_AUTHORITY);
    }

    @Provides
    @Named("contribution")
    public ContentProviderClient provideContributionContentProviderClient() {
        return application.getContentResolver().acquireContentProviderClient(CONTRIBUTION_AUTHORITY);
    }

    @Provides
    @Named("modification")
    public ContentProviderClient provideModificationContentProviderClient() {
        return application.getContentResolver().acquireContentProviderClient(MODIFICATIONS_AUTHORITY);
    }

    @Provides
    @Named("application_preferences")
    public SharedPreferences providesApplicationSharedPreferences() {
        return application.getSharedPreferences("fr.free.nrw.commons", MODE_PRIVATE);
    }

    @Provides
    @Named("default_preferences")
    public SharedPreferences providesDefaultSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(application);
    }

    @Provides
    @Named("prefs")
    public SharedPreferences providesOtherSharedPreferences() {
        return application.getSharedPreferences("prefs", MODE_PRIVATE);
    }

    @Provides
    public UploadController providesUploadController(SessionManager sessionManager, @Named("default_preferences") SharedPreferences sharedPreferences) {
        return new UploadController(sessionManager, application, sharedPreferences);
    }

    @Provides
    @Singleton
    public SessionManager providesSessionManager(MediaWikiApi mediaWikiApi) {
        return new SessionManager(application, mediaWikiApi);
    }

    @Provides
    @Singleton
    public MediaWikiApi provideMediaWikiApi() {
        return new ApacheHttpClientMediaWikiApi(BuildConfig.WIKIMEDIA_API_HOST);
    }

    @Provides
    @Singleton
    public LocationServiceManager provideLocationServiceManager() {
        return new LocationServiceManager(application);
    }

    @Provides
    @Singleton
    public CacheController provideCacheController() {
        return new CacheController();
    }

    @Provides
    @Singleton
    public DBOpenHelper provideDBOpenHelper() {
        return new DBOpenHelper(application);
    }

    @Provides
    @Singleton
    public NearbyPlaces provideNearbyPlaces() {
        return new NearbyPlaces();
    }

    @Provides
    @Singleton
    public LruCache<String, String> provideLruCache() {
        return new LruCache<>(1024);
    }
}
