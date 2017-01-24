package controllers

import java.io._

import com.typesafe.config.{Config, ConfigFactory}
import play.api.Logger
import play.api.data.Form
import play.api.libs.Files.TemporaryFile
import play.api.mvc.{MaxSizeExceeded, _}
import utils.{FlowHelper, FlowInfo, FlowInfoStorage}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.MultipartFormData.FilePart

class Upload extends Controller with FlowHelper {

    case class UploadMultiPart(flowCurrentChunkSize: Int,
                               flowTotalSize: Int,
                               flowIdentifier: String,
                               flowFilename: String,
                               flowRelativePath: String,
                               flowChunkNumber: Int,
                               flowTotalChunks: Int,
                               flowChunkSize: Int)
    val defaultConfig: Config = ConfigFactory.load()
    val FILE_MULTIPART_FIELD_NAME: String = "file"

    val uploadForm = Form(
        mapping(
        "flowCurrentChunkSize" -> number(min = 1),
        "flowTotalSize" -> number(min = 1),
        "flowIdentifier" -> nonEmptyText,
        "flowFilename" -> nonEmptyText,
        "flowRelativePath" -> nonEmptyText,
        "flowChunkNumber" -> number(min = 1),
        "flowTotalChunks" -> number(min = 1),
        "flowChunkSize" -> number(min =1)
        )(UploadMultiPart.apply)(UploadMultiPart.unapply)
    )

    def upload = Action(parse.multipartFormData) { implicit request =>
            uploadForm.bindFromRequest.fold(
                formWithErrors => {
                    BadRequest("Incorrect form data or form data not present")
                },
                uploadMultiPartData =>
                    request.body.file(FILE_MULTIPART_FIELD_NAME).map { file =>
/*                        file.contentType match {
                            case Some("image/jpeg") | Some("image/png") =>
                                Logger.info("File passed check - isImage")
                                val is = new FileInputStream(picture.ref.file)
                                dealWithFile(is, multipart)
                            case _ => BadRequest("invalid content type")*/
                        if(processFileAndReturnAreAllChunksUploaded(uploadMultiPartData, file.ref)) {
                            Ok("Upload for Chunk Complete and All Chunks Uploaded Successfully")
                        } else {
                            Ok("Upload for Chunk Complete")
                        }
                    }.getOrElse(
                        BadRequest("No file found in request"))
            )
    }

    private def processFileAndReturnAreAllChunksUploaded(uploadedMultiPartData: UploadMultiPart,
                            fileReference: TemporaryFile)(implicit request: RequestHeader):Boolean = {
        val currentChunkNumber = uploadedMultiPartData.flowChunkNumber

        val flowInfo: FlowInfo = FlowInfoStorage.getOrCreateFlowEntry(uploadedMultiPartData.flowIdentifier,
                                                                      uploadedMultiPartData.flowFilename,
                                                                      uploadedMultiPartData.flowRelativePath,
                                                                      uploadedMultiPartData.flowTotalChunks,
                                                                      uploadedMultiPartData.flowChunkSize)
        val contentLength: Long = request.headers("Content-Length").toLong

        val is = new FileInputStream(fileReference.file)
        writeInTempFile(currentChunkNumber, flowInfo, contentLength, is)
        Logger.debug(s"Successfully wrote the uploadedFile to: ${flowInfo.getTemporaryFilePathOnSystem}")
        flowInfo.setChunkNumberAsFullyUploaded(currentChunkNumber)
        if (flowInfo.isUploadeComplete) {
            moveFileToCompletedDirectory(flowInfo)
            FlowInfoStorage.removeFlowEntry(flowInfo.identifier)
            Logger.debug(s"All parts of the file have been successfully uploaded (${flowInfo.totalChunks} chunks)")
            true
        } else {
            false
        }
    }

    def uploadGet(chunkNumber: Int,
                  chunkSize: Int,
                  currentChunkSize: Int,
                  totalSize: Int,
                  identifier: String,
                  filename: String,
                  relativePath: String,
                  totalChunks: Int): Action[AnyContent] = Action.async { implicit request =>
        val info: FlowInfo = FlowInfoStorage.getOrCreateFlowEntry(identifier, filename, relativePath, totalChunks, chunkSize)
        info.isChunkNumberUploaded(chunkNumber) match {
            case Success(isChunkUploaded) =>
                if(isChunkUploaded) {
                    Logger.debug(s"Chunk $chunkNumber has already been uploaded")
                    Future(Ok("Chunk already uploaded"))
                }else {
                    // Use any non-permanent error
                    Logger.debug(s"Chunk $chunkNumber has NOT been uploaded")
                    Future(NotAcceptable("Chunk has not been uploaded"))
                }
            case Failure(e) =>
                Future(BadRequest(e.getMessage))
        }
    }
}
