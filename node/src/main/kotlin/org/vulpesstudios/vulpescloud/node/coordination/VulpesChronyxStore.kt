package org.vulpesstudios.vulpescloud.node.coordination

import org.vulpesstudios.chronyx.ChronyxStore
import org.vulpesstudios.chronyx.LockInfo
import org.vulpesstudios.vulpescloud.node.db.ChronyxLeaseRepository

/** SQL-backed distributed leases; each lease is a relational row keyed by task and token. */
class VulpesChronyxStore : ChronyxStore {
    override suspend fun acquire(taskName:String,hostId:String,maxGlobalInstances:Int,maxHostInstances:Int,leaseDurationMillis:Long):String? =
        ChronyxLeaseRepository.acquire(taskName,hostId,maxGlobalInstances,maxHostInstances,leaseDurationMillis)
    override suspend fun renew(taskName:String,hostId:String,token:String,leaseDurationMillis:Long):Boolean =
        ChronyxLeaseRepository.renew(taskName,hostId,token,leaseDurationMillis)
    override suspend fun release(taskName:String,hostId:String,token:String):Boolean = ChronyxLeaseRepository.release(taskName,hostId,token)
    override suspend fun getGlobalCount(taskName:String):Int = ChronyxLeaseRepository.globalCount(taskName)
    override suspend fun getHostCount(taskName:String,hostId:String):Int = ChronyxLeaseRepository.hostCount(taskName,hostId)
    override suspend fun getLockInfo(taskName:String):LockInfo? = ChronyxLeaseRepository.lockInfo(taskName)
}
