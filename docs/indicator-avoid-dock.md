# 应用列表白点与分页自适应说明

## 这次修改解决什么问题

当你把 `overview_indicator_middle_gap` 调大，或者把 dock 放大以后：

- 白点会往上挪；
- 应用列表底部留白也会变大；
- 但如果分页行数还是按旧高度算，就会出现“上面应用显示不全，需要往下划”的现象。

这次修改的核心，就是让 **dock 高度变化后，白点位置、应用列表底部留白、每页行数、分页内容** 一起重算。

## 修改文件

- `app/src/main/java/com/boringdroid/systemui/view/AppOverviewWindow.kt`
- `app/src/main/java/com/boringdroid/systemui/view/DockAppsLayout.kt`
- `app/src/main/res/values/dimens.xml`

## 类似 diff 的关键变化

```diff
+    var dockScaleFactor: Float = 1.0f
+        set(value) {
+            if (field == value) {
+                return
+            }
+            field = value
+            refreshOverviewLayout()
+        }
```

```diff
-        val dimensionPixelSize2 =
-            getContext().resources.getDimensionPixelSize(R.dimen.overview_margin_bottom)
+        updateIndicatorBottomMargin()
+        updateGridMetrics()
```

```diff
+    private fun getOverviewContentBottomInset(): Int {
+        val resources = getContext().resources
+        val dockHeight = resources.getDimensionPixelSize(R.dimen.dock_app_layout_height)
+        val middleGap = resources.getDimensionPixelSize(R.dimen.overview_indicator_middle_gap)
+        val indicatorHeight = resources.getDimensionPixelSize(R.dimen.overview_indicator_height)
+        val dockOffset = (dockHeight * dockScaleFactor + 0.5f).toInt()
+        return dockOffset + middleGap + indicatorHeight + middleGap
+    }
```

```diff
+    <dimen name="overview_row_spacing_min">1dp</dimen>
+    <dimen name="overview_indicator_middle_gap">4dp</dimen>
+    <dimen name="overview_indicator_height">16dp</dimen>
```

## 修改后的效果

- dock 变大后，白点会跟着上移；
- 应用列表底部留白会同步调整；
- 每页显示行数会按新的可用高度重新计算；
- 放不下的内容会自动分到下一页，而不是挤在当前页底部。