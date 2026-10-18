package com.sisaguna.android.ui.i18n

import com.sisaguna.android.data.settings.AppLanguage
import com.sisaguna.android.data.remote.ApiException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

class EnNewCopyTest {

    @Before
    fun english() {
        SgLocale.language = AppLanguage.EN
    }

    @After
    fun back() {
        SgLocale.language = AppLanguage.ID
    }

    @Test
    fun `a backend error reaches the user in English with its first letter capitalised`() {
        val shown = ApiException(409, "out_of_stock", "stok tidak cukup").userMessage
        assertEquals("Stok tidak cukup", shown)
        assertEquals("Not enough stock", tr(shown))
    }

    @Test
    fun `every backend message has an English form`() {
        // A message added to the backend without a translation would leak Indonesian into English mode.
        EnNewCopy.map.keys.filter { it.first().isUpperCase() }.forEach { assertNotEquals(it, tr(it)) }
    }

    @Test
    fun `notification texts from the server are translated`() {
        assertEquals("Order SG-1234 is ready for pickup", tr("Pesanan SG-1234 siap diambil"))
        assertEquals("Order SG-1 at Warung Budi must be collected within 20 minutes.", tr("Pesanan SG-1 di Warung Budi harus diambil dalam 20 menit."))
        assertEquals("Warung Budi cancelled your order. Reason: stok habis", tr("Warung Budi membatalkan pesananmu. Alasan: stok habis"))
        assertEquals("Warung Budi cancelled your order.", tr("Warung Budi membatalkan pesananmu."))
        assertEquals("A buyer gave 5 stars to Warung Budi.", tr("Pembeli memberi 5 bintang untuk Warung Budi."))
        assertEquals("Nasi Box, free. A seller you saved just added food.", tr("Nasi Box, gratis. Penyedia yang kamu simpan baru saja menambah makanan."))
    }

    @Test
    fun `templated admin copy`() {
        assertEquals("Reject Warung Baru?", tr("Tolak Warung Baru?"))
        assertEquals("20% (max. Rp 8.000) · min. Rp 15.000", tr("20% (maks. Rp 8.000) · min. Rp 15.000"))
        assertEquals("Used 2 of 50 · Valid until 18 Okt 2026", tr("Dipakai 2 dari 50 · Berlaku sampai 18 Okt 2026"))
    }

    @Test
    fun `indonesian mode leaves text alone`() {
        SgLocale.language = AppLanguage.ID
        assertEquals("Stok tidak cukup", tr("Stok tidak cukup"))
    }
}
