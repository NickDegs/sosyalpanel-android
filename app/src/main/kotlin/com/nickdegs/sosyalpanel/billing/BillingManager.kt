package com.nickdegs.sosyalpanel.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.nickdegs.sosyalpanel.data.AuthService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// iOS StoreManager (StoreKit 2) karşılığı — Google Play Billing 7.
// Abonelik ürünleri Play Console'da oluşturulur:
//   pro_monthly  → aylık abonelik
//   pro_yearly   → yıllık abonelik (base plan'lar tek subscription altında da olabilir)
class BillingManager(context: Context) : PurchasesUpdatedListener {

    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var localActive = false   // lokal Play entitlement (cache)

    init {
        LicenseService.init(context)
        scope.launch { LicenseService.loadConfig(); recompute() }
    }

    // Etkin Pro = sunucu-otoriteli gate (STRICT modda sunucu+giriş+online şart).
    private fun recompute() {
        _isPro.value = LicenseService.effectivePro(localActive, AuthService.isLoggedIn.value)
    }

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products

    private val client = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    fun start() {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProducts()
                    queryEntitlements()
                }
            }
            override fun onBillingServiceDisconnected() {}
        })
    }

    private fun queryProducts() {
        val ids = listOf("pro_monthly", "pro_yearly")
        val params = QueryProductDetailsParams.newBuilder().setProductList(
            ids.map {
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(it)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            }
        ).build()
        client.queryProductDetailsAsync(params) { result, list ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                _products.value = list
            }
        }
    }

    fun queryEntitlements() {
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val activePurchase = purchases.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                localActive = activePurchase != null
                recompute()
                purchases.forEach { acknowledge(it) }
                // Sunucu-otoriteli doğrulama (Play Integrity + Google makbuz).
                activePurchase?.let { p ->
                    val pid = p.products.firstOrNull() ?: ""
                    scope.launch { LicenseService.verify(pid, p.purchaseToken); recompute() }
                }
            }
        }
    }

    fun purchase(activity: Activity, product: ProductDetails) {
        val offer = product.subscriptionOfferDetails?.firstOrNull() ?: return
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(product)
                    .setOfferToken(offer.offerToken)
                    .build()
            )
        ).build()
        client.launchBillingFlow(activity, params)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { acknowledge(it) }
            queryEntitlements()
        }
    }

    private fun acknowledge(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            client.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
            ) {}
        }
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            localActive = true
            recompute()
        }
    }
}
