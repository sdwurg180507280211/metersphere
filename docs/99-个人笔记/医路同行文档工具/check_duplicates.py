
import os
import random
import zipfile
import tempfile

BASE_PATH = '/Users/edy/Desktop/百度网盘同步空间/其它/审核/20260417'
FOLDERS = [
    '单人会议1-150',
    '单人会议301-450',
    '单人会议600-796',
    '多人会议 51-100场'
]
MEETINGS_PER_FOLDER = 5


def find_images_in_folder(folder_path):
    keqian = None
    kezhong = None
    for root, dirs, files in os.walk(folder_path):
        for file in files:
            if file in ['课前.jpg', '课前.png'] and keqian is None:
                keqian = os.path.join(root, file)
            elif file in ['课中.jpg', '课中.png'] and kezhong is None:
                kezhong = os.path.join(root, file)
        if keqian and kezhong:
            break
    result = []
    if keqian and os.path.getsize(keqian) > 0:
        result.append(keqian)
    if kezhong and os.path.getsize(kezhong) > 0:
        result.append(kezhong)
    return result


def extract_from_zip(folder_path, num_meetings):
    zip_files = [f for f in os.listdir(folder_path) if f.endswith('.zip')]
    if not zip_files:
        return []
    extracted_images = []
    random.shuffle(zip_files)
    meetings_collected = 0
    for zip_file in zip_files:
        if meetings_collected >= num_meetings:
            break
        zip_path = os.path.join(folder_path, zip_file)
        try:
            with zipfile.ZipFile(zip_path, 'r') as zf:
                keqian = None
                kezhong = None
                for f in zf.namelist():
                    filename = os.path.basename(f)
                    if filename in ['课前.jpg', '课前.png'] and keqian is None:
                        keqian = f
                    elif filename in ['课中.jpg', '课中.png'] and kezhong is None:
                        kezhong = f
                    if keqian and kezhong:
                        break
                if keqian or kezhong:
                    meetings_collected += 1
                    if keqian:
                        temp_dir = tempfile.mkdtemp()
                        zf.extract(keqian, temp_dir)
                        extracted_path = os.path.join(temp_dir, keqian)
                        if os.path.exists(extracted_path) and os.path.getsize(extracted_path) > 0:
                            extracted_images.append((extracted_path, zip_file, os.path.basename(keqian)))
                    if kezhong:
                        temp_dir = tempfile.mkdtemp() if not keqian else temp_dir
                        zf.extract(kezhong, temp_dir)
                        extracted_path = os.path.join(temp_dir, kezhong)
                        if os.path.exists(extracted_path) and os.path.getsize(extracted_path) > 0:
                            extracted_images.append((extracted_path, zip_file, os.path.basename(kezhong)))
        except Exception as e:
            continue
    return extracted_images


def extract_from_subfolders(folder_path, num_meetings):
    subfolders = [f for f in os.listdir(folder_path)
                  if os.path.isdir(os.path.join(folder_path, f)) and not f.startswith('.')]
    if not subfolders:
        return []
    extracted_images = []
    random.shuffle(subfolders)
    meetings_collected = 0
    for subfolder in subfolders:
        if meetings_collected >= num_meetings:
            break
        subfolder_path = os.path.join(folder_path, subfolder)
        found_images = find_images_in_folder(subfolder_path)
        if found_images:
            meetings_collected += 1
            for img in found_images:
                extracted_images.append((img, subfolder, os.path.basename(img)))
    return extracted_images


def check_duplicates():
    all_images = []
    image_files = []
    filenames = []

    for folder in FOLDERS:
        print(f"\n=== {folder} ===")
        folder_path = os.path.join(BASE_PATH, folder)
        zip_files = [f for f in os.listdir(folder_path) if f.endswith('.zip')]
        if zip_files:
            images = extract_from_zip(folder_path, MEETINGS_PER_FOLDER)
        else:
            images = extract_from_subfolders(folder_path, MEETINGS_PER_FOLDER)
        all_images.extend(images)
        for img_path, source, filename in images:
            print(f"  {source} -> {filename}")
            image_files.append(img_path)
            filenames.append(filename)

    print(f"\n=== 重复检查 ===")
    seen = {}
    duplicates = []
    for i, (img_path, source, filename) in enumerate(all_images):
        key = (source, filename)
        if key in seen:
            duplicates.append((i, key, seen[key]))
        else:
            seen[key] = i

    if duplicates:
        print(f"发现 {len(duplicates)} 个重复项:")
        for dup in duplicates:
            print(f"  重复: {dup[1][0]} -> {dup[1][1]}")
    else:
        print("✓ 没有发现重复")

    filename_count = {}
    for fn in filenames:
        filename_count[fn] = filename_count.get(fn, 0) + 1
    print(f"\n文件名统计:")
    for fn, cnt in filename_count.items():
        print(f"  {fn}: {cnt} 张")


if __name__ == '__main__':
    check_duplicates()
