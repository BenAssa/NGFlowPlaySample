package utils

import scala.util.Try
import java.io.{File, InputStream, RandomAccessFile}

import com.typesafe.config.ConfigFactory

/**
  * User: Kayrnt
  * Date: 19/10/14
  * Time: 16:45
  */
case class FlowInfo(identifier: String,
                    filename: String,
                    relativePath: String,
                    totalChunks: Int,
                    chunkSize: Int) {

    private val uploadedChunks: Array[Boolean] = new Array[Boolean](totalChunks)

    def getTemporaryFilePathOnSystem: String = {
        val temporaryFileBaseDir: String = ConfigFactory.load().getString("TEMPORARY_UPLOADS_DIRECTORY")
        new File(temporaryFileBaseDir, filename).getAbsolutePath + ".temp"
    }

    def getCompletedFilePathOnSystem: String = {
        val completedFileBaseDir: String = ConfigFactory.load().getString("COMPLETED_UPLOADS_DIRECTORY")
        new File(completedFileBaseDir, filename).getAbsolutePath
    }

    def isUploadeComplete: Boolean = {
        //check if upload finished
        for(item <- uploadedChunks) {
            if(!item) {
                return false
            }
        }
        true

/*        //Upload finished, change filename.
        return true*/
    }

    /**
      * Returns whether the chunk number has already been downloaded. If the chunk number is bigger than the number of
      * expected chunks, then `ArrayOutOfBoundException` is sent in the Try
      * @param chunkNumber
      * @return
      */
    def isChunkNumberUploaded(chunkNumber: Int): Try[Boolean] = {
        val chunkNumber0BaseForArray = convert1BaseTo0Base(chunkNumber)
        Try(uploadedChunks(chunkNumber0BaseForArray))
    }

    /**
      * Sets the chunk number as downloaded. If the chunk number is larger than the expected number of chunks then
      * `false` is returned, otherwise `true` if the update was successful
      * @param chunkNumber
      * @return
      */
    def setChunkNumberAsFullyUploaded(chunkNumber: Int): Boolean = {
        if(chunkNumber > totalChunks) {
            false
        }else {
            val chunkNumber0BaseForArray = convert1BaseTo0Base(chunkNumber)
            uploadedChunks.update(chunkNumber0BaseForArray, true)
            true
        }
    }

    private def convert1BaseTo0Base(number: Int): Int = {
        number -1
    }
}

