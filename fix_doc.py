#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import re

def fix_document(input_file, output_file):
    with open(input_file, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    result = []
    i = 0
    in_toc = False
    in_table = False

    while i < len(lines):
        line = lines[i]

        # 检测目录开始
        if '目录' in line and line.strip() == '目录':
            result.append(line)
            result.append('\n')
            in_toc = True
            i += 1
            continue

        # 检测目录结束（遇到"引言"或其他一级标题）
        if in_toc and line.strip() and not line.strip()[0].isdigit() and '引言' in line:
            in_toc = False

        # 处理目录项，转换为Markdown格式
        if in_toc:
            stripped = line.strip()
            # 跳过空行
            if not stripped:
                result.append(line)
                i += 1
                continue

            # 计算前导空格数量
            leading_spaces = len(line) - len(line.lstrip())

            # 匹配目录项格式: "数字 标题 页码"
            # 三级目录标准: "   2.1.1  URL命名   5" (3个或更多空格，且有两个点和数字)
            match3 = re.match(r'^(\d+\.\d+\.\d+)\s+(.+?)\s+\d+\s*$', stripped)
            # 三级目录特殊: "   5.1.接口描述  8" (3个或更多空格，5.1.后面是汉字)
            match3_special = re.match(r'^(\d+\.\d+\.)(.+?)\s+\d+\s*$', stripped)
            # 二级目录: "  1.1  编写目的  5" 或 "  5.1账号登录接口    8" (2个空格，且有一个点)
            match2 = re.match(r'^(\d+\.\d+)\s*(.+?)\s+\d+\s*$', stripped)
            # 一级目录: "1   引言  5" 或 "4公共参数 7" (0-1个空格)
            match1 = re.match(r'^(\d+)\s+(.+?)\s+\d+\s*$', stripped)
            # 一级目录特殊格式: "4公共参数 7" (数字后直接是汉字)
            match1_special = re.match(r'^(\d+)([^\d\s].+?)\s+\d+\s*$', stripped)

            if match3 and leading_spaces >= 3:
                # 三级目录标准（3个或更多空格）
                num, title = match3.groups()
                result.append(f'      - [{num} {title}](#{num.replace(".", "")})\n')
            elif match3_special and leading_spaces >= 3:
                # 三级目录特殊格式（3个或更多空格，如 5.1.接口描述）
                num, title = match3_special.groups()
                result.append(f'      - [{num}{title}](#{num.replace(".", "")})\n')
            elif match2 and leading_spaces == 2:
                # 二级目录（正好2个空格）
                num, title = match2.groups()
                result.append(f'   - [{num} {title}](#{num.replace(".", "")})\n')
            elif match1 and leading_spaces <= 1:
                # 一级目录（0-1个空格）
                num, title = match1.groups()
                result.append(f'- [{num} {title}](#{num})\n')
            elif match1_special and leading_spaces == 0:
                # 一级目录特殊格式（无空格）
                num, title = match1_special.groups()
                result.append(f'- [{num} {title}](#{num})\n')
            else:
                result.append(line)
            i += 1
            continue

        # 检测表格开始（以|开头的行）
        if line.strip().startswith('|'):
            # 如果之前不在表格中，说明这是新表格的开始
            if not in_table:
                in_table = True
                # 检查前面是否已经有"字段说明："
                has_field_desc = False
                for j in range(max(0, i-5), i):
                    if '字段说明：' in lines[j] or '字段说明:' in lines[j]:
                        has_field_desc = True
                        break

                # 检查前一行是否为空行或者是否在文档开头部分（前50行）
                # 文档开头的表格不需要添加"字段说明："
                if not has_field_desc and i > 50:
                    result.append('字段说明：\n')
        else:
            # 如果当前行不是表格行，说明表格结束
            if in_table:
                in_table = False

        result.append(line)
        i += 1

    # 写入输出文件
    with open(output_file, 'w', encoding='utf-8') as f:
        f.writelines(result)

    print(f"文档处理完成！输出文件：{output_file}")

if __name__ == '__main__':
    fix_document('接口设计文档_utf8.txt', '接口设计文档_fixed.md')
