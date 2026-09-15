import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.ServiceList

fun main() {
    val info = ChannelInfo.getInfo(ServiceList.YouTube, "https://youtube.com/channel/UC-lHJZR3Gqxm24_Vd_AJ5Yw")
    println(info)
}
