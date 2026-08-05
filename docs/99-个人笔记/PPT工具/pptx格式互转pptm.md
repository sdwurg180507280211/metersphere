# PPTX 批量转换为 PPTM

## 目标
- 自动遍历 `/Users/zhaozhiwei/Desktop/baiduwangpan/工作/20260515/PPT`
- 将所有 `.pptx` 批量另存为真正可支持宏的 `.pptm`
- 自动创建目标目录
- 输出成功 / 失败统计

## WPS 宏代码

```js
function batchSaveAsPptm() {
    const srcFolder = "/Users/zhaozhiwei/Desktop/baiduwangpan/工作/20260515/PPT/";
    const dstFolder = "/Users/zhaozhiwei/Desktop/baiduwangpan/工作/20260515/pptm/";
    const PPTM = 25; // 启用宏的演示文稿

    // 自动创建目标目录
    if (!Dir(dstFolder)) {
        MkDir(dstFolder);
    }

    let success = 0;
    let fail = 0;
    let failedFiles = [];

    let file = Dir(srcFolder + "*.pptx");

    while (file) {
        if (!file.startsWith("~$") && !file.startsWith(".~")) {
            try {
                console.log("转换：" + file);

                const pres = Application.Presentations.Open(srcFolder + file);
                pres.SaveAs(dstFolder + file.replace(/\.pptx$/i, ".pptm"), PPTM);
                pres.Close();

                success++;
            } catch (e) {
                fail++;
                failedFiles.push(file + " => " + e.message);
                console.log("失败：" + file + "，错误：" + e.message);
            }
        }

        file = Dir();
    }

    console.log("转换完成");
    console.log("成功：" + success);
    console.log("失败：" + fail);

    if (failedFiles.length > 0) {
        console.log("失败文件：");
        for (let i = 0; i < failedFiles.length; i++) {
            console.log(failedFiles[i]);
        }
    }
}
```

## 说明
- 这里的转换方式是通过 WPS 重新保存为 `.pptm`，不是简单改后缀。
- 转换后文件具备宏格式，但宏代码仍需后续在 WPS 宏编辑器中自行加入。
- 如果目标目录已存在同名文件，WPS 可能会报错；需要时可先清空目标目录。

---

# PPTM 批量转换为 PPTX

## 目标
- 自动遍历 `/Users/zhaozhiwei/Desktop/baiduwangpan/工作/20260515/pptm`
- 将所有 `.pptm` 批量另存为普通 `.pptx`
- 自动创建目标目录
- 输出成功 / 失败统计

## WPS 宏代码

```js
function batchSaveAsPptx() {
    const srcFolder = "/Users/zhaozhiwei/Desktop/baiduwangpan/工作/20260515/pptm/";
    const dstFolder = "/Users/zhaozhiwei/Desktop/baiduwangpan/工作/20260515/pptx/";
    const PPTX = 24; // 普通演示文稿

    // 自动创建目标目录
    if (!Dir(dstFolder)) {
        MkDir(dstFolder);
    }

    let success = 0;
    let fail = 0;
    let failedFiles = [];

    let file = Dir(srcFolder + "*.pptm");

    while (file) {
        if (!file.startsWith("~$") && !file.startsWith(".~")) {
            try {
                console.log("转换：" + file);

                const pres = Application.Presentations.Open(srcFolder + file);
                pres.SaveAs(dstFolder + file.replace(/\.pptm$/i, ".pptx"), PPTX);
                pres.Close();

                success++;
            } catch (e) {
                fail++;
                failedFiles.push(file + " => " + e.message);
                console.log("失败：" + file + "，错误：" + e.message);
            }
        }

        file = Dir();
    }

    console.log("转换完成");
    console.log("成功：" + success);
    console.log("失败：" + fail);

    if (failedFiles.length > 0) {
        console.log("失败文件：");
        for (let i = 0; i < failedFiles.length; i++) {
            console.log(failedFiles[i]);
        }
    }
}
```

## 说明
- 这里的转换方式是通过 WPS 重新保存为 `.pptx`。
- `pptm -> pptx` 会去掉宏，得到普通演示文稿。
- 如果目标目录已存在同名文件，WPS 可能会报错；需要时可先清空目标目录。
