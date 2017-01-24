package utils

import java.io.{File, InputStream, RandomAccessFile}

import com.typesafe.config.ConfigFactory
import play.api.Logger
import play.api.libs.Files.TemporaryFile
import play.api.mvc.{MultipartFormData, RequestHeader}

trait FlowHelper {

    protected def getFlowChunkNumber(request: RequestHeader): Int =
        request.getQueryString("flowChunkNumber").fold(-1)(_.toInt)

    def moveFileToCompletedDirectory(flowInfo: FlowInfo): Unit = {
        val file: File = new File(flowInfo.getTemporaryFilePathOnSystem)
        file.renameTo(new File(flowInfo.getCompletedFilePathOnSystem))
        Logger.debug(s"Moved ${flowInfo.getTemporaryFilePathOnSystem} to ${flowInfo.getCompletedFilePathOnSystem}")
    }

    protected def writeInTempFile(flowChunkNumber: Int,
                                  flowInfo: FlowInfo,
                                  contentLength: Long,
                                  input: InputStream):Unit = {
        val raf: RandomAccessFile = new RandomAccessFile(flowInfo.getTemporaryFilePathOnSystem, "rw")
        raf.seek((flowChunkNumber - 1) * flowInfo.chunkSize)
        var readData: Long = 0
        val bytes: Array[Byte] = new Array[Byte](1024 * 100)
        var r: Int = 0
        do {
            r = input.read(bytes)
            if (r > 0) {
                raf.write(bytes, 0, r)
                readData += r
            }
        }
        while (readData < contentLength && r > 0)
        raf.close()
    }

}

