package com.sisaguna.android.data.remote

import com.sisaguna.android.data.remote.dto.AddressDto
import com.sisaguna.android.data.remote.dto.AddressRequest
import com.sisaguna.android.data.remote.dto.AdminComplaintDto
import com.sisaguna.android.data.remote.dto.AdminMerchantDto
import com.sisaguna.android.data.remote.dto.AdminVoucherDto
import com.sisaguna.android.data.remote.dto.CancelBody
import com.sisaguna.android.data.remote.dto.CreateVoucherBody
import com.sisaguna.android.data.remote.dto.PatchVoucherBody
import com.sisaguna.android.data.remote.dto.UpdateComplaintBody
import com.sisaguna.android.data.remote.dto.ChangePasswordRequest
import com.sisaguna.android.data.remote.dto.ComplaintBody
import com.sisaguna.android.data.remote.dto.ConfirmBody
import com.sisaguna.android.data.remote.dto.ConversationDto
import com.sisaguna.android.data.remote.dto.MessageDto
import com.sisaguna.android.data.remote.dto.ReadBody
import com.sisaguna.android.data.remote.dto.SendMessageBody
import com.sisaguna.android.data.remote.dto.StartConversationBody
import com.sisaguna.android.data.remote.dto.WsTicketDto
import com.sisaguna.android.data.remote.dto.CreateMerchantBody
import com.sisaguna.android.data.remote.dto.DefaultPaymentBody
import com.sisaguna.android.data.remote.dto.DeviceBody
import com.sisaguna.android.data.remote.dto.LinkPaymentBody
import com.sisaguna.android.data.remote.dto.MerchantDashboardDto
import com.sisaguna.android.data.remote.dto.NotificationDto
import com.sisaguna.android.data.remote.dto.NotificationPrefsDto
import com.sisaguna.android.data.remote.dto.OrderDto
import com.sisaguna.android.data.remote.dto.PaymentMethodDto
import com.sisaguna.android.data.remote.dto.PlaceOrderBody
import com.sisaguna.android.data.remote.dto.ReviewBody
import com.sisaguna.android.data.remote.dto.ReviewDto
import com.sisaguna.android.data.remote.dto.VoucherDto
import com.sisaguna.android.data.remote.dto.CreateListingRequest
import com.sisaguna.android.data.remote.dto.ForgotPasswordBody
import com.sisaguna.android.data.remote.dto.HomeDto
import com.sisaguna.android.data.remote.dto.ImpactDto
import com.sisaguna.android.data.remote.dto.ListingCardDto
import com.sisaguna.android.data.remote.dto.ListingDetailDto
import com.sisaguna.android.data.remote.dto.LoginRequest
import com.sisaguna.android.data.remote.dto.MediaDto
import com.sisaguna.android.data.remote.dto.MerchantDto
import com.sisaguna.android.data.remote.dto.MyListingDto
import com.sisaguna.android.data.remote.dto.RefreshRequest
import com.sisaguna.android.data.remote.dto.ResetPasswordBody
import com.sisaguna.android.data.remote.dto.RegisterRequest
import com.sisaguna.android.data.remote.dto.SessionDto
import com.sisaguna.android.data.remote.dto.UserDto
import kotlinx.serialization.json.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/** sisaguna-be `/api/v1`. Paths mirror `sisaguna-be/apiendpoints/MOBILE.md` §8. */
interface ApiService {

    // ---- Auth
    @POST("auth/register") suspend fun register(@Body body: RegisterRequest): Envelope<SessionDto>
    @POST("auth/refresh") suspend fun refresh(@Body body: RefreshRequest): Envelope<SessionDto>
    /** Always answers 202, whether or not the email exists, so accounts can't be probed. */
    @POST("auth/forgot-password") suspend fun forgotPassword(@Body body: ForgotPasswordBody)
    @POST("auth/reset-password") suspend fun resetPassword(@Body body: ResetPasswordBody): Envelope<SessionDto>
    @POST("auth/login") suspend fun login(@Body body: LoginRequest): Envelope<SessionDto>
    /** Takes the header explicitly: the local session is already cleared when this runs. */
    @POST("auth/logout") suspend fun logout(@Header("Authorization") authorization: String, @Body body: RefreshRequest)
    @POST("auth/password") suspend fun changePassword(@Body body: ChangePasswordRequest): Envelope<SessionDto>

    // ---- Me
    @GET("me") suspend fun me(): Envelope<UserDto>

    /** Body is a [JsonObject] because `photo_media_id: null` (remove the photo) must be sent explicitly. */
    @PATCH("me") suspend fun updateMe(@Body body: JsonObject): Envelope<UserDto>

    @GET("me/impact") suspend fun impact(): Envelope<ImpactDto>

    @GET("me/addresses") suspend fun addresses(): Envelope<List<AddressDto>>
    @POST("me/addresses") suspend fun createAddress(@Body body: AddressRequest): Envelope<AddressDto>
    @PATCH("me/addresses/{id}") suspend fun updateAddress(@Path("id") id: String, @Body body: AddressRequest): Envelope<AddressDto>
    @DELETE("me/addresses/{id}") suspend fun deleteAddress(@Path("id") id: String)

    /** `{"address_id": null}` selects the GPS location, hence [JsonObject]. */
    @PUT("me/selected-address") suspend fun selectAddress(@Body body: JsonObject)

    @Multipart
    @POST("media")
    suspend fun upload(@Part("purpose") purpose: RequestBody, @Part file: MultipartBody.Part): Envelope<MediaDto>

    // ---- Catalog (public; a token only adds `is_saved`)
    @GET("home")
    suspend fun home(@Query("lat") lat: Double?, @Query("lng") lng: Double?, @Query("max_km") maxKm: Double? = null): Envelope<HomeDto>

    @GET("listings")
    suspend fun listings(
        @Query("tier") tier: String?,
        @Query("sort") sort: String? = null,
        @Query("q") query: String? = null,
        @Query("free") free: Boolean? = null,
        @Query("max_km") maxKm: Double? = null,
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = 50,
    ): Envelope<List<ListingCardDto>>

    @GET("listings/{id}")
    suspend fun listing(@Path("id") id: String, @Query("lat") lat: Double?, @Query("lng") lng: Double?): Envelope<ListingDetailDto>

    @GET("merchants")
    suspend fun merchants(
        @Query("lat") lat: Double?,
        @Query("lng") lng: Double?,
        @Query("available") available: Boolean? = true,
    ): Envelope<List<MerchantDto>>

    @GET("me/saved-merchants")
    suspend fun savedMerchants(@Query("lat") lat: Double?, @Query("lng") lng: Double?): Envelope<List<MerchantDto>>

    @PUT("me/saved-merchants/{id}") suspend fun saveMerchant(@Path("id") id: String)
    @DELETE("me/saved-merchants/{id}") suspend fun unsaveMerchant(@Path("id") id: String)

    @GET("me/merchant")
    suspend fun myMerchant(): Envelope<MerchantDto>

    @GET("me/merchant/listings")
    suspend fun myListings(@Query("limit") limit: Int? = 50): Envelope<List<MyListingDto>>

    @POST("listings")
    suspend fun createListing(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateListingRequest,
    ): Envelope<ListingDetailDto>

    @DELETE("listings/{id}") suspend fun deleteListing(@Path("id") id: String)

    @POST("merchants") suspend fun createMerchant(@Body body: CreateMerchantBody): Envelope<MerchantDto>

    @GET("merchants/{id}/reviews")
    suspend fun merchantReviews(@Path("id") id: String, @Query("cursor") cursor: String? = null, @Query("limit") limit: Int? = 50): Envelope<List<ReviewDto>>

    // ---- Orders (buyer)
    @POST("orders")
    suspend fun placeOrder(@Header("Idempotency-Key") idempotencyKey: String, @Body body: PlaceOrderBody): Envelope<OrderDto>

    @GET("me/orders")
    suspend fun orders(
        @Query("tab") tab: String?,
        @Query("filter") filter: String? = null,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int? = 50,
    ): Envelope<List<OrderDto>>

    @GET("orders/{id}") suspend fun order(@Path("id") id: String): Envelope<OrderDto>
    @POST("orders/{id}/cancel") suspend fun cancelOrder(@Path("id") id: String, @Body body: CancelBody): Envelope<OrderDto>
    @POST("orders/{id}/review") suspend fun reviewOrder(@Path("id") id: String, @Body body: ReviewBody): Envelope<ReviewDto>
    @POST("orders/{id}/complaint") suspend fun complainOrder(@Path("id") id: String, @Body body: ComplaintBody): Envelope<JsonObject>

    // ---- Orders (merchant)
    @GET("me/merchant/orders")
    suspend fun merchantOrders(@Query("status") status: String?, @Query("cursor") cursor: String? = null, @Query("limit") limit: Int? = 50): Envelope<List<OrderDto>>

    @POST("me/merchant/orders/{id}/confirm") suspend fun confirmOrder(@Path("id") id: String, @Body body: ConfirmBody): Envelope<OrderDto>
    @POST("me/merchant/orders/{id}/cancel") suspend fun merchantCancelOrder(@Path("id") id: String, @Body body: CancelBody): Envelope<OrderDto>
    @GET("me/merchant/dashboard") suspend fun dashboard(): Envelope<MerchantDashboardDto>

    // ---- Payment methods and vouchers
    @GET("me/payment-methods") suspend fun paymentMethods(): Envelope<List<PaymentMethodDto>>
    @POST("me/payment-methods") suspend fun linkPaymentMethod(@Body body: LinkPaymentBody): Envelope<List<PaymentMethodDto>>
    @PUT("me/payment-methods/default") suspend fun setDefaultPaymentMethod(@Body body: DefaultPaymentBody): Envelope<List<PaymentMethodDto>>
    @DELETE("me/payment-methods/{kind}") suspend fun unlinkPaymentMethod(@Path("kind") kind: String): Envelope<List<PaymentMethodDto>>

    // ---- Chat
    @GET("conversations")
    suspend fun conversations(@Query("as") side: String, @Query("limit") limit: Int? = 50): Envelope<List<ConversationDto>>

    @POST("conversations") suspend fun startConversation(@Body body: StartConversationBody): Envelope<ConversationDto>

    @GET("conversations/{id}/messages")
    suspend fun messages(
        @Path("id") id: String,
        @Query("before_id") beforeId: Long? = null,
        @Query("after_id") afterId: Long? = null,
        @Query("limit") limit: Int? = 50,
    ): Envelope<List<MessageDto>>

    @POST("conversations/{id}/messages") suspend fun sendMessage(@Path("id") id: String, @Body body: SendMessageBody): Envelope<MessageDto>
    @POST("conversations/{id}/read") suspend fun readConversation(@Path("id") id: String, @Body body: ReadBody)
    @POST("ws/ticket") suspend fun wsTicket(): Envelope<WsTicketDto>

    // ---- Admin (403 for anyone without is_admin)
    @GET("admin/merchants")
    suspend fun adminMerchants(@Query("status") status: String?, @Query("page") page: Int, @Query("limit") limit: Int = 20): Envelope<List<AdminMerchantDto>>

    @POST("admin/merchants/{id}/approve") suspend fun approveMerchant(@Path("id") id: String)
    @POST("admin/merchants/{id}/reject") suspend fun rejectMerchant(@Path("id") id: String)

    @GET("admin/complaints")
    suspend fun adminComplaints(@Query("status") status: String?, @Query("page") page: Int, @Query("limit") limit: Int = 20): Envelope<List<AdminComplaintDto>>

    @PATCH("admin/complaints/{id}") suspend fun updateComplaint(@Path("id") id: String, @Body body: UpdateComplaintBody)

    @GET("admin/vouchers")
    suspend fun adminVouchers(@Query("page") page: Int, @Query("limit") limit: Int = 20): Envelope<List<AdminVoucherDto>>

    @POST("admin/vouchers") suspend fun createVoucher(@Body body: CreateVoucherBody): Envelope<AdminVoucherDto>
    @PATCH("admin/vouchers/{code}") suspend fun patchVoucher(@Path("code") code: String, @Body body: PatchVoucherBody): Envelope<AdminVoucherDto>

    // ---- Notifications
    @GET("me/notifications")
    suspend fun notifications(@Query("cursor") cursor: String? = null, @Query("limit") limit: Int? = 50): Envelope<List<NotificationDto>>

    @PATCH("me/notifications/{id}") suspend fun markNotificationRead(@Path("id") id: String, @Body body: JsonObject)
    @POST("me/notifications/read-all") suspend fun markAllNotificationsRead()
    @DELETE("me/notifications/{id}") suspend fun deleteNotification(@Path("id") id: String)
    @POST("me/notifications/{id}/restore") suspend fun restoreNotification(@Path("id") id: String)
    @PUT("me/devices") suspend fun registerDevice(@Body body: DeviceBody)

    /** Takes the header explicitly: used during logout, when the local session is already cleared. */
    @DELETE("me/devices/{token}")
    suspend fun unregisterDevice(@Header("Authorization") authorization: String, @Path("token") token: String)

    @GET("me/notification-prefs") suspend fun notificationPrefs(): Envelope<NotificationPrefsDto>
    @PUT("me/notification-prefs") suspend fun setNotificationPrefs(@Body body: NotificationPrefsDto)

    @GET("me/vouchers") suspend fun vouchers(): Envelope<List<VoucherDto>>
    @POST("vouchers/{code}/claim") suspend fun claimVoucher(@Path("code") code: String)

    @GET("merchants/{id}")
    suspend fun merchant(@Path("id") id: String, @Query("lat") lat: Double?, @Query("lng") lng: Double?): Envelope<MerchantDto>
}

/** Token refresh is deliberately a separate client without the authenticator, so a rejected refresh can't recurse. */
interface RefreshApi {
    @POST("auth/refresh")
    fun refresh(@Body body: RefreshRequest): Call<Envelope<SessionDto>>
}
