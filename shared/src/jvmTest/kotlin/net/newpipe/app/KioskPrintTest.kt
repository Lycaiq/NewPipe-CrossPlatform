package net.newpipe.app
import org.schabi.newpipe.extractor.ServiceList
import kotlin.test.Test

class KioskPrintTest {
    @Test
    fun testKiosks() {
        val list = ServiceList.YouTube.kioskList
        println("AVAILABLE KIOSKS: ")
        list.getList().forEach {
            println("ID: ${it.id} -> URL: ${it.url}")
        }
    }
}
