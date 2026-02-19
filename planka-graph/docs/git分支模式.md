# pgraph Git分支管理模式

## 分支说明

### 主要分支
- **master**: 主开发分支，包含最新的开发代码，例如新特性和缺陷修复代码都优先在master分支上进行
- **release/x.x.x**: 发布分支，用于版本发布和生产环境部署
- **feature/xxx**: 特性分支，用于大型特性开发（可选）

### 工作流程

#### 1. 日常开发
```bash
# 在master分支进行开发
git checkout master
git pull origin master
# 进行开发...
git add .
git commit -m "feat: 新功能描述"
git push origin master
```

#### 2. 发布流程
```bash
# 从master创建release分支
git checkout master
git checkout -b release/1.1.0
git push origin release/1.1.0

```

#### 3. 热修复流程（推荐策略）

**策略A：Master优先修复（推荐）**
```bash
# 1. 优先在master分支修复（保证主干最新）
git checkout master
git pull origin master
# 修复问题...
git add .
git commit -m "fix: 修复XX问题"
git push origin master

# 2. Cherry-pick到需要的release分支
git checkout release/1.0.0
git cherry-pick <commit-hash>
# 如果有冲突，手动解决后继续
git cherry-pick --continue
git push origin release/1.0.0

# 3. 如果有多个release分支需要修复
git checkout release/1.1.0
git cherry-pick <commit-hash>
git push origin release/1.1.0
```

**策略B：Release分支修复（谨慎使用）**
```bash
# 仅在紧急情况下使用，且修复内容与master分支变化无关
git checkout release/1.0.0
# 修复问题...
git commit -m "hotfix: 紧急修复XX问题"
git push origin release/1.0.0

# 立即同步到master（必须操作）
git checkout master
git cherry-pick <commit-hash>
# 处理冲突（如果有）
git push origin master

# 同步到其他release分支
git checkout release/1.1.0
git cherry-pick <commit-hash>
git push origin release/1.1.0
```
