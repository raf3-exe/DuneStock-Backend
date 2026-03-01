package com.dunestock.api.controller

import com.dunestock.api.model.Membership
import com.dunestock.api.model.Membership.MembershipId
import com.dunestock.api.repository.MembershipRepository
import com.dunestock.api.repository.UserRepository
import com.dunestock.api.repository.WarehousesRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/memberships")
class MembershipController(
    private val membershipRepository: MembershipRepository,
    private val userRepository: UserRepository,
    private val warehouseRepository: WarehousesRepository
) {

    @PostMapping
    fun createMembership(
        @RequestParam userId: String,
        @RequestParam warehouseId: String,
        @RequestParam role: Membership.Role
    ): ResponseEntity<Map<String, String>> {

        val id = MembershipId(userId, warehouseId)

        if (membershipRepository.existsById(id)) {
            return ResponseEntity.status(409)
                .body(mapOf("message" to "Member already exists"))
        }

        val user = userRepository.findById(userId)
            .orElseThrow { RuntimeException("User not found") }

        val warehouse = warehouseRepository.findById(warehouseId)
            .orElseThrow { RuntimeException("Warehouse not found") }

        val membership = Membership().apply {
            this.id = id
            this.user = user
            this.warehouse = warehouse
            this.role = role
        }

        membershipRepository.save(membership)

        return ResponseEntity.ok(
            mapOf("message" to "Member added successfully")
        )
    }



    @PutMapping("/{userId}/{warehouseId}/role")
    fun updateRole(
        @PathVariable userId: String,
        @PathVariable warehouseId: String,
        @RequestParam role: Membership.Role
    ): ResponseEntity<Map<String, String>> {
        val id = MembershipId(userId, warehouseId)
        val membership = membershipRepository.findById(id)
            .orElseThrow { RuntimeException("Membership not found") }

        membership.role = role
        membershipRepository.save(membership)
        return ResponseEntity.ok(mapOf("message" to "Role updated successfully"))
    }



    data class WarehouseResponse(val warehouseId: String)

    @GetMapping("/my-warehouse/{userId}")
    fun getMyWarehouse(@PathVariable userId: String): ResponseEntity<WarehouseResponse> {

        // 💡 1. ดึงข้อมูล User ขึ้นมาก่อน (ใช้ userRepository ที่มีอยู่แล้ว)
        val userOptional = userRepository.findById(userId)

        if (userOptional.isPresent) {
            val user = userOptional.get()

            // เช็คว่า User คนนี้เป็น "เจ้าของ" โกดังไหนไหม? (ดึงจาก List ในโมเดล User ได้เลย)
            val ownedWarehouses = user.ownedWarehouses
            if (!ownedWarehouses.isNullOrEmpty()) {
                val ownerWarehouseId = ownedWarehouses[0].warehouseId
                return ResponseEntity.ok(WarehouseResponse(ownerWarehouseId))
            }
        }

        // 💡 2. ถ้าไม่ได้เป็นเจ้าของ ค่อยมาหาว่าเป็น "พนักงาน (Member)" ในโกดังไหน?
        val memberships = membershipRepository.findByUserUserId(userId)

        if (memberships.isNotEmpty()) {
            val memberWarehouseId = memberships[0].warehouse.warehouseId
            return ResponseEntity.ok(WarehouseResponse(memberWarehouseId))
        }

        // 3. ถ้าไม่เป็นทั้งเจ้าของและไม่ได้เป็นพนักงาน คืนค่า 404 (ไม่พบข้อมูล)
        return ResponseEntity.notFound().build()
    }




    // นำเข้า DTO ไว้ด้านบนหรือล่างของไฟล์
    data class MemberResponseDto(
        val username: String,
        val role: String,
        val userId: String
    )

    // เพิ่มฟังก์ชันนี้ลงในคลาส MembershipController
    @GetMapping("/warehouse/{warehouseId}")
    fun getMembersInWarehouse(@PathVariable warehouseId: String): ResponseEntity<List<MemberResponseDto>> {
        val memberships = membershipRepository.findByWarehouseWarehouseId(warehouseId)

        // แปลง Entity เป็น DTO เพื่อส่งกลับเฉพาะข้อมูลที่ Android ต้องการ
        val responseList = memberships.map { membership ->
            MemberResponseDto(
                username = membership.user.username, // 💡 ตรวจสอบให้แน่ใจว่าโมเดล User ของคุณมีฟิลด์ username
                role = membership.role.name, // แปลง Enum เป็น String (เช่น "E", "V")
                userId = membership.user.userId
            )
        }

        return ResponseEntity.ok(responseList)
    }


}