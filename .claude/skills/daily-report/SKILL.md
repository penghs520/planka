# 日报生成 Skill

根据 Git 提交记录生成格式化的工作日报，支持多仓库汇总。

## 使用方法

用户请求生成日报时，执行以下步骤：

### 步骤1：扫描仓库

检查扫描目录配置文件 `~/.config/daily-report/repos.txt`，自动发现其下所有 git 仓库：

```bash
CONFIG_FILE="$HOME/.config/daily-report/repos.txt"
if [ -f "$CONFIG_FILE" ]; then
  echo "扫描目录配置: $CONFIG_FILE"
  # 遍历配置的每个目录，查找其下所有 git 仓库
  while IFS= read -r dir || [ -n "$dir" ]; do
    [[ "$dir" =~ ^#.*$ || -z "$dir" ]] && continue
    expanded_dir="${dir/#\~/$HOME}"
    # 查找目录下所有包含 .git 的仓库（深度4层）
    find "$expanded_dir" -maxdepth 4 -type d -name ".git" 2>/dev/null | while read gitdir; do
      echo "$(dirname "$gitdir")"
    done
  done < "$CONFIG_FILE"
else
  echo "未找到配置文件，使用当前仓库"
fi
```

配置文件格式（每行一个扫描目录）：
```txt
# ~/.config/daily-report/repos.txt
# 指定要扫描的上层目录，会自动发现其下的 git 仓库
~/workspaces/sources/agilean
~/workspaces/personal
/path/to/projects
```

如果配置文件不存在，则只使用当前仓库。

### 步骤2：获取提交记录

根据用户指定的时间范围（默认昨天）遍历所有发现的仓库获取 Git 提交记录：

```bash
# 单仓库（当前目录）
git log --since="DATE 00:00:00" --until="DATE 23:59:59" --author="$(git config user.name)" --no-merges --oneline --stat

# 多仓库遍历（替换 DATE 为实际日期，格式：YYYY-MM-DD）
CONFIG_FILE="$HOME/.config/daily-report/repos.txt"
if [ -f "$CONFIG_FILE" ]; then
  # 先收集所有仓库路径
  repos=()
  while IFS= read -r dir || [ -n "$dir" ]; do
    [[ "$dir" =~ ^#.*$ || -z "$dir" ]] && continue
    expanded_dir="${dir/#\~/$HOME}"
    while IFS= read -r gitdir; do
      repos+=("$(dirname "$gitdir")")
    done < <(find "$expanded_dir" -maxdepth 4 -type d -name ".git" 2>/dev/null)
  done < "$CONFIG_FILE"

  # 遍历每个仓库获取提交
  for repo in "${repos[@]}"; do
    commits=$(git -C "$repo" log --since="DATE 00:00:00" --until="DATE 23:59:59" \
      --author="$(git config user.name)" --no-merges --oneline 2>/dev/null)
    if [ -n "$commits" ]; then
      echo ""
      echo "=== $(basename "$repo") ==="
      git -C "$repo" log --since="DATE 00:00:00" --until="DATE 23:59:59" \
        --author="$(git config user.name)" --no-merges --oneline --stat
    fi
  done
fi
```

### 步骤3：统计代码行数

**注意：跳过 Merge 提交**

```bash
# 单仓库统计（跳过 merge 提交）
# 先获取非 merge 提交的哈希列表，再统计
git log --since="DATE 00:00:00" --until="DATE 23:59:59" --author="$(git config user.name)" --no-merges --pretty=format:"%H" | while read hash; do
  git show "$hash" --stat --oneline
done | grep -E "files? changed" | awk '{for(i=1;i<=NF;i++){if($i~/insertion/){ins+=$(i-1)}; if($i~/deletion/){del+=$(i-1)}}} END {print "新增: "ins" 行, 删除: "del" 行, 净增: "(ins-del)" 行"}'

# 多仓库统计 - 对每个仓库分别执行上述命令，然后汇总
```

### 步骤4：生成日报

#### 单仓库格式（10-15行，适合截图）

```markdown
**工作日报 - YYYY年MM月DD日**

**工作量**: X小时 | N次提交 | 新增XXX行/删除XXX行（净增XXX行）

**1. 功能分类1（Xh）** - 关键产出描述 + 代码行数 + 主要文件

**2. 功能分类2（Xh）** - 关键产出描述 + 代码行数 + 主要文件

**3. Bug修复（Xh）** - 修复问题描述 + 涉及文件

**关键产出**: 核心成果总结
```

#### 多仓库格式（按项目分组）

```markdown
**工作日报 - YYYY年MM月DD日**

**工作量**: X小时 | N次提交（跨M个项目）| 新增XXX行/删除XXX行（净增XXX行）

**项目A** (N次提交, +XXX/-XXX行)
- feat: 功能描述
- fix: 修复描述

**项目B** (N次提交, +XXX/-XXX行)
- refactor: 重构描述

**关键产出**: 核心成果总结
```

## 分类规则

根据提交信息前缀分类：
- `feat:` / `feature:` → 新功能开发
- `fix:` → Bug修复
- `refactor:` → 代码重构
- `docs:` → 文档更新
- `test:` → 测试相关
- `perf:` → 性能优化
- `chore:` → 其他杂项

## 工时估算规则

基于代码行数和复杂度估算：
- 简单修改（<50行）：0.5-1小时
- 中等修改（50-200行）：1-2小时
- 复杂修改（200-500行）：2-4小时
- 大型功能（>500行）：4-8小时

## 时间范围选项

用户可指定：
- `昨天` / `yesterday`：前一天的提交
- `今天` / `today`：当天的提交
- `本周` / `this week`：本周的提交
- `最近N天`：最近N天的提交
- `指定日期 YYYY-MM-DD`：特定日期的提交

## 详细程度选项

- **简版**：5-8行，只列核心工作和产出
- **标准版**：10-15行，包含详细分类和数据（默认）
- **详细版**：不限行数，包含所有提交的文件清单

## 输出要求

1. 使用 Markdown 格式
2. 使用加粗突出关键信息
3. 单仓库总结控制在10-15行内
4. 多仓库按项目分组，每个项目简洁列出
5. 最后一行汇总关键产出
6. 数据准确（提交数、代码行数从 Git 获取）
7. **跳过 Merge 提交**：统计时自动排除 `Merge branch` 或 `Merge pull request` 类的提交

## 配置管理

### 添加扫描目录

```bash
mkdir -p ~/.config/daily-report
echo "~/workspaces/sources/agilean" >> ~/.config/daily-report/repos.txt
echo "~/workspaces/personal" >> ~/.config/daily-report/repos.txt
```

### 查看当前配置

```bash
cat ~/.config/daily-report/repos.txt
```

### 预览会扫描到哪些仓库

```bash
while IFS= read -r dir || [ -n "$dir" ]; do
  [[ "$dir" =~ ^#.*$ || -z "$dir" ]] && continue
  expanded_dir="${dir/#\~/$HOME}"
  find "$expanded_dir" -maxdepth 4 -type d -name ".git" 2>/dev/null | while read gitdir; do
    echo "$(dirname "$gitdir")"
  done
done < ~/.config/daily-report/repos.txt
```

### 移除配置（恢复单仓库模式）

```bash
rm ~/.config/daily-report/repos.txt
```