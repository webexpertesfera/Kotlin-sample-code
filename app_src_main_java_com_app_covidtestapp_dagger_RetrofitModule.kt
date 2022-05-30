package com.app.tour_travel.dagger;
import android.accounts.AccountManager
import android.content.Context
import android.util.Log
import com.app.covidtestapp.dagger.NetworkRequests
import com.app.covidtestapp.utils.BASE_URL
import dagger.Module
import dagger.Provides
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.security.*
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton
import javax.net.ssl.*


/**
 * Created by Meddy.
 */
@Module
class RetrofitModule(val context: Context) {

    companion object {}

    @Provides
    @Singleton
    @Named("WithoutAccessToken")
    fun provideOkhttpClientWithoutAccessToken( ): OkHttpClient {
        val okHttpBuilder       : OkHttpClient.Builder      =  OkHttpClient.Builder()
        val interceptorLogging  : HttpLoggingInterceptor =  HttpLoggingInterceptor()
        interceptorLogging.level = HttpLoggingInterceptor.Level.BODY //turn to none in order to hide the messages from Retrofit in logcat
        val interceptor         : Interceptor = Interceptor {
            it.proceed(
                    it.request().newBuilder()
                            .addHeader("Accept", "application/json").build()
            )
        }
        okHttpBuilder.addInterceptor(interceptor)
        val okHttpClient    = okHttpBuilder.readTimeout(2, TimeUnit.MINUTES).writeTimeout(
                2,
                TimeUnit.MINUTES
        ).build()
        return okHttpClient
    }



    @Provides
    @Singleton
    @Named("WithAccessToken")
    fun provideOkhttpClientWithAccessToken(
            accountManager: AccountManager,
            @Named("pref_module") prefUtils: SharePrefUtils
    ): OkHttpClient {
        val interceptorLogging      = HttpLoggingInterceptor()
        interceptorLogging.level    = HttpLoggingInterceptor.Level.BODY
        val okHttpBuilder           = OkHttpClient.Builder()
      //  val strAuthorizationKey = accessToken
        //add headers
      val interceptor=  Interceptor { chain ->
          val request: Request

          request = if (prefUtils.getUserInfo()!=null) {
                chain.request().newBuilder() // .header("Accept", "application/json")
                    .header("Authorization", prefUtils.getUserInfo()!!.token)
                    .build()
            } else {
                chain.request().newBuilder().build()
            }
            chain.proceed(request)
        }
        okHttpBuilder.addInterceptor(interceptor)

        var okHttpClient=  okHttpBuilder
            .addInterceptor(interceptor)
            .connectTimeout(2, TimeUnit.MINUTES)
            .readTimeout(2, TimeUnit.MINUTES)
            .writeTimeout(2, TimeUnit.MINUTES)

        return okHttpClient.build()
    }

    @Provides
    @Singleton
    fun provideAccountManager(context: Context): AccountManager {
       return AccountManager.get(context)
    }


    @Provides
    @Singleton
    @Named("RetrofitWithoutAccessToken")
    fun provideRetrofitWithoutAccessToken(
            @Named("WithoutAccessToken") okHttpClient: OkHttpClient,
            context: Context,
            @Named("pref_module") prefUtils: SharePrefUtils
    ): Retrofit {
        return Retrofit.Builder()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .client(getUnsafeOkHttpClient(provideHeaderInterceptor(prefUtils)).build())
                .build()
    }




    private fun provideHeaderInterceptor(prefUtils:SharePrefUtils): Interceptor? {
        return Interceptor { chain ->

            val request: Request

            request = if (prefUtils.getUserInfo()!=null) {
                chain.request().newBuilder() // .header("Accept", "application/json")
                        .header("Authorization", "Token " + prefUtils.getUserInfo()!!.token!!)
                        .build()
            } else {
                chain.request().newBuilder().build()
            }
            chain.proceed(request)
        }
    }


    @Provides
    @Named("RykdomServiceWithAccessToken")
    @Singleton
    fun provideItemServiceWithoutAccessToken(@Named("RetrofitWithAccessToken") retrofit: Retrofit): NetworkRequests {
        return retrofit.create<NetworkRequests>(NetworkRequests::class.java!!)
    }

    @Provides
    @Named("RykdomServiceWithoutAccessToken")
    @Singleton
    fun  provideItemServiceWithAccessToken(@Named("RetrofitWithoutAccessToken") retrofit: Retrofit): NetworkRequests {
        return retrofit.create<NetworkRequests>(NetworkRequests::class.java!!)
    }

    fun getUnsafeOkHttpClient(provideHeaderInterceptor: Interceptor?): OkHttpClient.Builder {
        try {
            // Create a trust manager that does not validate certificate chains
            val builder = OkHttpClient.Builder()
            builder.connectTimeout(2, TimeUnit.MINUTES) // connect timeout

            builder.readTimeout(2, TimeUnit.MINUTES)

            return builder
        } catch (e: Exception) {
            throw e
        }
    }
}

