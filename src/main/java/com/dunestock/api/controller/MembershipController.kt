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
        @RequestBody body: Map<String, String>
    ): ResponseEntity<Map<String, String>> {
        val userId      = body["user_id"]      ?: return ResponseEntity.badRequest().body(mapOf("message" to "missing user_id"))
        val warehouseId = body["warehouse_id"] ?: return ResponseEntity.badRequest().body(mapOf("message" to "missing warehouse_id"))
        val roleStr     = body["role"]         ?: return ResponseEntity.badRequest().body(mapOf("message" to "missing role"))

        val role = try {
            Membership.Role.valueOf(roleStr) // เช่น E หรือ V
        } catch (e: Exception) {
            return ResponseEntity.badRequest().body(mapOf("message" to "invalid role: $roleStr"))
        }

        val id = MembershipId(userId, warehouseId)

        if (membershipRepository.existsById(id)) {
            return ResponseEntity.status(409).body(mapOf("message" to "Member already exists"))
        }

        val user = userRepository.findById(userId).orElseThrow { RuntimeException("User not found") }
        val warehouse = warehouseRepository.findById(warehouseId).orElseThrow { RuntimeException("Warehouse not found") }

        val membership = Membership().apply {
            this.id = id
            this.user = user
            this.warehouse = warehouse
            this.role = Membership.Role.W       // 🛑 บังคับเป็น 'W' เพื่อให้ต้องกดรับก่อน
            this.intendedRole = role            // ✅ ฝากตำแหน่งจริง (E/V) ไว้ที่นี่
            this.status = "PENDING"             // ⏳ เซ็ตสถานะเป็นรอกดยอมรับ
        }

        membershipRepository.save(membership)
        return ResponseEntity.ok(mapOf("message" to "Member added successfully"))
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
        val userOptional = userRepository.findById(userId)

        if (userOptional.isPresent) {
            val user = userOptional.get()
            val ownedWarehouses = user.ownedWarehouses
            if (!ownedWarehouses.isNullOrEmpty()) {
                val ownerWarehouseId = ownedWarehouses[0].warehouseId
                return ResponseEntity.ok(WarehouseResponse(ownerWarehouseId))
            }
        }

        val memberships = membershipRepository.findByUserUserId(userId)
        if (memberships.isNotEmpty()) {
            val memberWarehouseId = memberships[0].warehouse.warehouseId
            return ResponseEntity.ok(WarehouseResponse(memberWarehouseId))
        }

        return ResponseEntity.notFound().build()
    }

    data class MemberResponseDto(
        val username: String,
        val role: String,
        val userId: String
    )

    @GetMapping("/warehouse/{warehouseId}")
    fun getMembersInWarehouse(@PathVariable warehouseId: String): ResponseEntity<List<MemberResponseDto>> {
        val memberships = membershipRepository.findByWarehouseWarehouseId(warehouseId)
        val responseList = memberships.map { membership ->
            MemberResponseDto(
                username = membership.user.username,
                role     = membership.role.name,
                userId   = membership.user.userId
            )
        }
        return ResponseEntity.ok(responseList)
    }




//    @DeleteMapping("/{userId}/{warehouseId}")
//    fun deleteMembership(
//        @PathVariable userId: String,
//        @PathVariable warehouseId: String
//    ): ResponseEntity<Map<String, String>> {
//        val id = MembershipId(userId, warehouseId)
//
//        if (!membershipRepository.existsById(id)) {
//            return ResponseEntity.status(404).body(mapOf("message" to "Membership not found"))
//        }
//
//        membershipRepository.deleteById(id)
//        return ResponseEntity.ok(mapOf("message" to "Membership deleted (Invitation cancelled)"))
//    }

    @DeleteMapping("/{userId}/{warehouseId}")
    fun deleteMembership(
        @PathVariable userId: String,
        @PathVariable warehouseId: String
    ): ResponseEntity<Map<String, String>> {

        println("DELETE membership userId=$userId warehouseId=$warehouseId")

        val id = MembershipId(userId, warehouseId)

        if (!membershipRepository.existsById(id)) {
            println("❌ membership not found")
            return ResponseEntity.status(404).body(mapOf("message" to "Membership not found"))
        }

        membershipRepository.deleteById(id)
        println("✅ membership deleted")

        return ResponseEntity.ok(mapOf("message" to "Membership deleted"))
    }



//    @PutMapping("/{userId}/{warehouseId}/accept")
//    fun acceptMembership(
//        @PathVariable userId: String,
//        @PathVariable warehouseId: String
//    ): ResponseEntity<Map<String, String>> {
//        val id = MembershipId(userId, warehouseId)
//        val membership = membershipRepository.findById(id)
//            .orElseThrow { RuntimeException("Invitation not found") }
//
//        // 🚨 1. เพิ่มบรรทัดนี้ เพื่อแอบดูค่าก่อนที่จะถูกเปลี่ยน
//        println("👉 ก่อนกดยอมรับ: Role=${membership.role}, IntendedRole=${membership.intendedRole}")
//
//        membership.role = membership.intendedRole ?: Membership.Role.V
//
//        // 🚨 2. เพิ่มบรรทัดนี้ เพื่อดูว่าตกลงมันเปลี่ยนเป็นอะไรกันแน่
//        println("👉 หลังกดยอมรับ: สรุปเปลี่ยน Role เป็น = ${membership.role}")
//
//        membershipRepository.save(membership)
//
//        return ResponseEntity.ok(mapOf("message" to "Accepted successfully"))
//    }

    @PutMapping("/{userId}/{warehouseId}/accept")
    fun acceptMembership(
        @PathVariable userId: String,
        @PathVariable warehouseId: String
    ): ResponseEntity<Map<String, String>> {
        val id = MembershipId(userId, warehouseId)
        val membership = membershipRepository.findById(id)
            .orElseThrow { RuntimeException("Invitation not found") }

        // ✅ ย้ายค่าจากตำแหน่งที่ "ตั้งใจจะให้" (intendedRole) มาเป็น "ตำแหน่งจริง" (role)
        // ถ้าตอนเชิญเลือก 'E' ค่า membership.role ก็จะเปลี่ยนจาก 'W' กลายเป็น 'E' ทันที
        membership.role = membership.intendedRole ?: Membership.Role.V

        // ✅ เปลี่ยนสถานะเป็น ACTIVE เพื่อบอกว่าการเชื่อมต่อสมบูรณ์แล้ว
        membership.status = "ACTIVE"

        membershipRepository.save(membership)
        return ResponseEntity.ok(mapOf("message" to "Accepted successfully"))
    }





}