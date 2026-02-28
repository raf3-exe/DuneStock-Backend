package com.dunestock.api.repository

import com.dunestock.api.model.Membership
import com.dunestock.api.model.Membership.MembershipId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository


@Repository
interface MembershipRepository : JpaRepository<Membership, Membership.MembershipId> {
    fun findByUserUserId(userId: String): Membership?

    // ✅ เพิ่มฟังก์ชันนี้เพื่อดึง Membership ทั้งหมดของคลังสินค้านั้น
    fun findByWarehouseWarehouseId(warehouseId: String): List<Membership>

}

