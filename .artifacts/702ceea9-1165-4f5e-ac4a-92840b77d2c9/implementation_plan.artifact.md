# Implementation Plan - Fix Build Errors

The project currently fails to compile due to several issues ranging from missing imports to type mismatches and unresolved references. This plan outlines the steps to resolve these errors and ensure a successful build.

## User Review Required

> [!IMPORTANT]
> Some UI components (`MetricCard`, `DonutChartCard`, `TableTabsSection`, `MonthSpinner`, `YearSpinner`) are referenced in `AdminDashboardScreen.kt` and `ExpensesScreen.kt` but are not defined in the project. I will implement basic versions of these components to resolve the unresolved reference errors.

## Proposed Changes

### [API & Data Layer]

#### [MODIFY] [ApiService.kt](file:///C:/Users/Arn/Documents/Codes/LaundryInventorySystemAPP/app/src/main/java/com/laundry/app/api/ApiService.kt)
- Change all occurrences of `Call<Void>` to `Call<Unit>` for better Kotlin interoperability.

#### [MODIFY] [LaundryRepository.kt](file:///C:/Users/Arn/Documents/Codes/LaundryInventorySystemAPP/app/src/main/java/com/laundry/app/repository/LaundryRepository.kt)
- Update code to handle `Result<Unit>` instead of `Result<Void>`.
- Fix type mismatches where `Void` was inferred but `Unit` was expected.

### [UI Components]

#### [NEW] [CommonComponents.kt](file:///C:/Users/Arn/Documents/Codes/LaundryInventorySystemAPP/app/src/main/java/com/laundry/app/ui/screens/CommonComponents.kt)
- Implement missing components: `MetricCard`, `DonutChartCard`, `TableTabsSection`, `MonthSpinner`, `YearSpinner`.

### [UI Screens]

#### [MODIFY] [MainActivity.kt](file:///C:/Users/Arn/Documents/Codes/LaundryInventorySystemAPP/app/src/main/java/com/laundry/app/MainActivity.kt)
- Add missing `import androidx.compose.foundation.shape.RoundedCornerShape`. (Already applied)

#### [MODIFY] [BranchesScreen.kt](file:///C:/Users/Arn/Documents/Codes/LaundryInventorySystemAPP/app/src/main/java/com/laundry/app/ui/screens/BranchesScreen.kt)
- Add `import androidx.lifecycle.viewModelScope`.
- Add `import kotlinx.coroutines.launch`.

#### [MODIFY] [ExpensesScreen.kt](file:///C:/Users/Arn/Documents/Codes/LaundryInventorySystemAPP/app/src/main/java/com/laundry/app/ui/screens/ExpensesScreen.kt)
- Add `import androidx.lifecycle.viewModelScope`.
- Add `import kotlinx.coroutines.launch`.
- Fix `trailing` parameter in `TextField` (should be `trailingIcon`).

#### [MODIFY] [InventoryScreen.kt](file:///C:/Users/Arn/Documents/Codes/LaundryInventorySystemAPP/app/src/main/java/com/laundry/app/ui/screens/InventoryScreen.kt)
- Add `import androidx.lifecycle.viewModelScope`.
- Add `import kotlinx.coroutines.launch`.

#### [MODIFY] [ServicesScreen.kt](file:///C:/Users/Arn/Documents/Codes/LaundryInventorySystemAPP/app/src/main/java/com/laundry/app/ui/screens/ServicesScreen.kt)
- Add `import androidx.lifecycle.viewModelScope`.
- Add `import kotlinx.coroutines.launch`.
- Add `import com.laundry.app.ui.theme.TealCyanDark`.

#### [MODIFY] [UserManagementScreen.kt](file:///C:/Users/Arn/Documents/Codes/LaundryInventorySystemAPP/app/src/main/java/com/laundry/app/ui/screens/UserManagementScreen.kt)
- Add `import androidx.lifecycle.viewModelScope`.
- Add `import kotlinx.coroutines.launch`.
- Add `import com.laundry.app.ui.theme.LaundryBlueLight`.

#### [MODIFY] [EmployeeDashboardScreen.kt](file:///C:/Users/Arn/Documents/Codes/LaundryInventorySystemAPP/app/src/main/java/com/laundry/app/ui/screens/EmployeeDashboardScreen.kt)
- Add `import com.laundry.app.ui.theme.LaundryBlueLight`.
- Fix `Icon` call by moving `size` to `modifier`.

#### [MODIFY] [AdminDashboardScreen.kt](file:///C:/Users/Arn/Documents/Codes/LaundryInventorySystemAPP/app/src/main/java/com/laundry/app/ui/screens/AdminDashboardScreen.kt)
- Add imports for the new common components.
- Fix unresolved reference `it` by explicitly naming lambda parameters if needed.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:assembleDebug` to verify the project builds successfully.

### Manual Verification
- N/A (Build verification is sufficient for these errors).
