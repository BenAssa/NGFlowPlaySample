package utils

import scala.collection.parallel.mutable.ParHashMap

object FlowInfoStorage {
    private var mMap: ParHashMap[String, FlowInfo] = new ParHashMap[String, FlowInfo]()

    /**
      * Get ResumableInfo from mMap or Create a new one.
      * @param flowIdentifier
      * @param flowFileName
      * @param flowRelativePath
      * @param flowTotalChunks
      * @return
      */
    def getOrCreateFlowEntry(flowIdentifier: String,
                             flowFileName: String,
                             flowRelativePath: String,
                             flowTotalChunks: Int,
                             flowChunkSize: Int): FlowInfo = {
        mMap.getOrElse(flowIdentifier, {
            val newFlowEntry = FlowInfo(flowIdentifier, flowFileName, flowRelativePath, flowTotalChunks, flowChunkSize)
            mMap += (flowIdentifier -> newFlowEntry)
            newFlowEntry
        })
    }

    /**
      * Removes the flow entry
      * @param flowIdentifier
      * @return
      */
    def removeFlowEntry(flowIdentifier: String) =  mMap -= flowIdentifier
}

