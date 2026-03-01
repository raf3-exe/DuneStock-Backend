package com.dunestock.api.repository

import com.dunestock.api.model.Membership
import com.dunestock.api.model.Warehouse
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository


@Repository
interface MembershipRepository : JpaRepository<Membership, Membership.MembershipId> {

    fun findByUserUserId(userId: String): List<Membership>

    // ✅ เพิ่มฟังก์ชันนี้เพื่อดึง Membership ทั้งหมดของคลังสินค้านั้น
    fun findByWarehouseWarehouseId(warehouseId: String): List<Membership>

    // สมมติว่าในโมเดล Warehouse คุณใช้ฟิลด์ owner ผูกกับตาราง User
    interface WarehousesRepository : JpaRepository<Warehouse, String> {

        // เพิ่มคำสั่งนี้เพื่อหาว่า User คนนี้เป็นเจ้าของโกดังไหน
        fun findByOwnerUserId(userId: String): List<Warehouse>
    }

    fun findByUserUserIdAndRole(userId: String?, role: Membership.Role?): MutableList<Membership?>?

}

