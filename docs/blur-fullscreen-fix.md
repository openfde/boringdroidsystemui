# 应用列表高斯模糊全屏修复说明

## 修改目标

解决在调整 `wm density` 之后，应用列表背景模糊范围变小、不能铺满全屏的问题。

## 修改文件

- `app/src/main/res/layout/layout_all_app_overview.xml`

## 原来的写法

```xml
<ImageView
    android:id="@+id/bg_iv"
    android:layout_width="2112dp"
    android:layout_height="1188dp"
    android:layout_marginTop="-54dp"
    android:layout_marginLeft="-96dp"
    android:scaleType="centerCrop" />
```

## 修改后的写法

```xml
<ImageView
    android:id="@+id/bg_iv"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:scaleType="centerCrop" />
```

## 类似 diff 的变化

```diff
-    android:layout_width="2112dp"
-    android:layout_height="1188dp"
-    android:layout_marginTop="-54dp"
-    android:layout_marginLeft="-96dp"
+    android:layout_width="match_parent"
+    android:layout_height="match_parent"
```

## 原因说明

原来背景模糊层使用固定 `dp` 尺寸。
当你执行 `wm density 120` 之后，`dp` 到 `px` 的换算发生变化，背景图实际渲染范围会变小，所以看起来像“模糊范围缩小了”。

改成 `match_parent` 以后：

- 模糊背景始终跟随父布局大小；
- 不再依赖固定 `dp`；
- 在不同 density 下都能铺满整个应用列表页面。