package com.jerusha.mplotgpos

data class BillItem(
    val itemName: String,
    val quantity: Int,
    val unitPrice: Double,
    val discount: Double = 0.0  // discount percentage
) {
    fun getSubtotal(): Double = quantity * unitPrice
    fun getDiscountAmount(): Double = getSubtotal() * (discount / 100.0)
    fun getTotal(): Double = getSubtotal() - getDiscountAmount()
}

data class Bill(
    val shopName: String = "ABC STORE",
    val shopAddress: String = "123 Main Street",
    val shopPhone: String = "+1-800-123-4567",
    val billNumber: String = "",
    val date: String = "",
    val time: String = "",
    val items: List<BillItem> = emptyList(),
    val notes: String = ""
) {
    fun getSubtotal(): Double = items.sumOf { it.getSubtotal() }
    fun getTotalDiscount(): Double = items.sumOf { it.getDiscountAmount() }
    fun getGrandTotal(): Double = items.sumOf { it.getTotal() }
    fun getItemCount(): Int = items.size
}
