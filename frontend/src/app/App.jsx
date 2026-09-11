import { useEffect, useState } from 'react'
import Editor from '@monaco-editor/react'
import { appConfig } from '../config/appConfig'
import { changePassword, createProblem, createTestCase, createTopic, getActivity, getBadges, getCurrentUser, getLeaderboard, getProblems, getSubmission, getSubmissions, getTopics, login, logout, register, runCode, submitSolution, updateProfile } from '../services/api/endpoints'
import { getItem, setItem } from '../services/storage/localStorage'

function Icon({ children }) {
  return <span className="icon" aria-hidden="true">{children}</span>
}

function UserAvatar({ user, className = 'avatar avatar-violet' }) {
  return user.profilePhoto
    ? <img className={`${className} avatar-photo`} src={user.profilePhoto} alt={`${user.username} profile`} />
    : <div className={className}>{user.username.slice(0, 2).toUpperCase()}</div>
}

function SplashScreen() {
  return (
    <div className="splash-screen" role="status" aria-label="Loading code2career">
      <div className="splash-stars" />
    </div>
  )
}

function AuthScreen({ mode, setMode, onAuthenticated }) {
  const [form, setForm] = useState({ username: '', email: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleChange = (event) => {
    setForm({ ...form, [event.target.name]: event.target.value })
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')
    setLoading(true)
    try {
      const user = mode === 'register' ? await register(form) : await login(form)
      onAuthenticated(user)
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'We could not complete that request. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-shell">
      <div className="auth-glow auth-glow-one" /><div className="auth-glow auth-glow-two" />
      <div className="auth-brand"><img className="brand-logo" src="/c2c-logo.png" alt="C2C" /><div><strong>code<span>2</span>career</strong><small>LEVEL UP YOUR CRAFT</small></div></div>
      <div className="auth-layout">
        <div className="auth-pitch"><p className="eyebrow">THE DEVELOPER GUILD</p><h1>Build skills.<br /><span>Forge your legend.</span></h1><p className="auth-description">A focused arena for ambitious developers. Solve real problems, build your streak, and rise through the ranks.</p><div className="auth-proof"><div><strong>2.4k+</strong><small>active learners</small></div><div><strong>38k</strong><small>quests cleared</small></div><div><strong>98%</strong><small>feel stronger</small></div></div></div>
        <div className="auth-card"><div className="auth-tabs"><button className={mode === 'login' ? 'selected' : ''} onClick={() => setMode('login')}>Sign in</button><button className={mode === 'register' ? 'selected' : ''} onClick={() => setMode('register')}>Create account</button></div><p className="eyebrow">{mode === 'login' ? 'WELCOME BACK, WARRIOR' : 'JOIN THE GUILD'}</p><h2>{mode === 'login' ? 'Enter the arena' : 'Begin your quest'}</h2><p className="muted">{mode === 'login' ? 'Your next level is waiting.' : 'Create your profile and earn your first XP.'}</p><form onSubmit={handleSubmit}>{mode === 'register' && <input name="username" placeholder="Username" value={form.username} onChange={handleChange} required minLength="3" />}<input name="email" type="email" placeholder="Email address" value={form.email} onChange={handleChange} required /><input name="password" type="password" placeholder="Password" value={form.password} onChange={handleChange} required minLength="8" /><button className="primary-button auth-submit" disabled={loading}>{loading ? 'Loading...' : mode === 'login' ? 'Enter dashboard →' : 'Create my profile →'}</button></form>{error && <p className="auth-error">{error}</p>}<small className="auth-note">By continuing, you agree to the guild rules and privacy policy.</small></div>
      </div>
    </div>
  )
}

function ChallengesView({ topics, problems, loading, onTopicChange, onSelectProblem }) {
  const [query, setQuery] = useState('')
  const [difficulty, setDifficulty] = useState('ALL')
  const filteredProblems = problems.filter((problem) => {
    const matchesQuery = problem.title.toLowerCase().includes(query.toLowerCase())
    const matchesDifficulty = difficulty === 'ALL' || problem.difficulty === difficulty
    return matchesQuery && matchesDifficulty
  })

  return (
    <div className="challenges-view">
      <div className="challenge-heading"><div><p className="eyebrow">THE TRAINING GROUNDS</p><h1>Choose your next <span>challenge.</span></h1><p className="muted">Sharpen your instincts with focused problems built for real progress.</p></div><div className="challenge-count"><strong>{problems.length}</strong><small>available quests</small></div></div>
      <div className="challenge-toolbar"><div className="search-box">⌕<input placeholder="Search challenges..." value={query} onChange={(event) => setQuery(event.target.value)} /></div><select onChange={(event) => onTopicChange(event.target.value)} defaultValue=""><option value="">All topics</option>{topics.map((topic) => <option value={topic.id} key={topic.id}>{topic.name}</option>)}</select><div className="difficulty-tabs">{['ALL', 'EASY', 'MEDIUM', 'HARD'].map((level) => <button className={difficulty === level ? 'selected' : ''} onClick={() => setDifficulty(level)} key={level}>{level}</button>)}</div></div>
        {loading ? <div className="empty-state">Loading your next quests...</div> : filteredProblems.length === 0 ? <div className="empty-state">No challenges match your current filters.</div> : <div className="challenge-list">{filteredProblems.map((problem, index) => <article className="challenge-card" key={problem.id}><div className="challenge-number">{String(index + 1).padStart(2, '0')}</div><div className="challenge-main"><div className="challenge-tags"><span className={`difficulty ${problem.difficulty.toLowerCase()}`}>{problem.difficulty}</span><span>+{problem.xpReward} XP</span></div><h3>{problem.title}</h3><p>{problem.description}</p><div className="challenge-meta"><span>◈ Problem #{problem.id}</span><span>⌁ Ready to solve</span></div></div><button className="solve-button" onClick={() => onSelectProblem(problem)}>Solve quest <span>→</span></button></article>)}</div>}
    </div>
  )
}

function SolverView({ problem, onBack, onSubmissionUpdated }) {
    const defaultCode = 'import java.util.Scanner;\n\npublic class Solution {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int a = sc.nextInt();\n        int b = sc.nextInt();\n        System.out.println(a + b);\n        sc.close();\n    }\n}'
    const [code, setCode] = useState(() => getItem(`${appConfig.draftStoragePrefix}${problem.id}`) || defaultCode)
    const [customInput, setCustomInput] = useState('')
    const [output, setOutput] = useState('')
    const [status, setStatus] = useState('')
    const [running, setRunning] = useState(false)

    useEffect(() => {
      setItem(`${appConfig.draftStoragePrefix}${problem.id}`, code)
    }, [code, problem.id])

    const configureJavaEditor = (monaco) => {
      monaco.languages.registerCompletionItemProvider('java', {
        triggerCharacters: ['.'],
        provideCompletionItems: (model, position) => {
          const line = model.getLineContent(position.lineNumber).slice(0, position.column - 1)
          const scannerItems = /\bsc\.$/.test(line)
            ? [
              ['nextInt()', 'nextInt();', 'Read the next integer'],
              ['nextLine()', 'nextLine();', 'Read the next line'],
              ['nextDouble()', 'nextDouble();', 'Read the next decimal number'],
              ['hasNextInt()', 'hasNextInt()', 'Check whether an integer is available'],
              ['close()', 'close();', 'Close the scanner'],
            ]
            : []
          const items = [
            ...scannerItems,
            ['Scanner import', 'import java.util.Scanner;\\n\\n', 'Add the Scanner import'],
            ['main method', 'public static void main(String[] args) {\\n\\t$0\\n}', 'Create a Java entry point'],
            ['for loop', 'for (int i = 0; i < $1; i++) {\\n\\t$0\\n}', 'Create a for loop'],
            ['System.out.println', 'System.out.println($0);', 'Print a line'],
          ]
          return {
            suggestions: items.map(([label, insertText, documentation]) => ({
              label,
              kind: monaco.languages.CompletionItemKind.Snippet,
              insertText,
              insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
              documentation,
              range: {
                startLineNumber: position.lineNumber,
                startColumn: position.column,
                endLineNumber: position.lineNumber,
                endColumn: position.column,
              },
            })),
          }
        },
      })
    }

    const handleRun = async () => {
      setRunning(true)
      setStatus('Running code...')
      try {
        const normalizedInput = customInput.replace(/,\s*/g, '\n')
        const result = await runCode(code, normalizedInput)
        setOutput(typeof result === 'string' ? result : result.output || JSON.stringify(result, null, 2))
        setStatus('Execution complete')
      } catch (error) {
        setOutput(error.response?.data?.message || 'Code runner is unavailable.')
        setStatus('Execution failed')
      } finally {
        setRunning(false)
      }
    }

    const handleSubmit = async () => {
      setRunning(true)
      setStatus('Submitting for evaluation...')
      try {
        const result = await submitSolution(problem.id, code)
        setOutput(`Submission #${result.id} is ${result.status}. Your result will appear in activity.`)
        setStatus('Submitted')
        for (let attempt = 0; attempt < 6; attempt += 1) {
          await new Promise((resolve) => setTimeout(resolve, 1500))
          const updated = await getSubmission(result.id)
          onSubmissionUpdated(updated)
          if (updated.status !== 'PENDING') {
            setOutput(`Submission #${updated.id}: ${updated.status}`)
            setStatus(updated.status === 'ACCEPTED' ? 'Quest cleared' : 'Try again')
            break
          }
        }
      } catch (error) {
        setOutput(error.response?.data?.message || 'Submission failed. Please try again.')
        setStatus('Submission failed')
      } finally {
        setRunning(false)
      }
    }

    return (
      <div className="solver-view">
        <button className="back-link" onClick={onBack}>← Back to challenges</button>
        <div className="solver-layout">
          <section className="problem-description panel"><div className="challenge-tags"><span className={`difficulty ${problem.difficulty.toLowerCase()}`}>{problem.difficulty}</span><span>+{problem.xpReward} XP</span></div><h1>{problem.title}</h1><p>{problem.description}</p>{problem.constraints && <><h4>Constraints</h4><p className="constraints">{problem.constraints}</p></>}<div className="solver-tip"><span>✦</span><div><strong>Quest tip</strong><p>Test your idea with custom input before submitting.</p></div></div></section>
          <section className="editor-panel panel"><div className="editor-toolbar"><span><i /> Solution.java</span><select defaultValue="java"><option value="java">Java 17</option></select></div><Editor height="360px" language="java" theme="vs-dark" value={code} beforeMount={configureJavaEditor} onChange={(value) => setCode(value || '')} options={{ automaticLayout: true, minimap: { enabled: false }, fontSize: 13, lineNumbers: 'on', tabSize: 4, suggestOnTriggerCharacters: true, quickSuggestions: true, padding: { top: 16 } }} /><div className="input-label">CUSTOM INPUT</div><textarea className="custom-input" placeholder={'Example: 15\n25 (commas also supported)'} value={customInput} onChange={(event) => setCustomInput(event.target.value)} /><div className="editor-actions"><span className="run-status">{status}</span><button className="run-button" onClick={handleRun} disabled={running}>▶ Run code</button><button className="submit-button" onClick={handleSubmit} disabled={running}>Submit solution <span>→</span></button></div>{output && <pre className="output-panel">{output}</pre>}</section>
        </div>
      </div>
    )
}

function LeaderboardView({ entries, currentUser }) {
  return <div className="feature-view"><div className="feature-heading"><div><p className="eyebrow">THE HALL OF FAME</p><h1>Rise through the <span>ranks.</span></h1><p className="muted">Every solved problem moves you closer to the top.</p></div><div className="challenge-count"><strong>TOP 10</strong><small>global ranking</small></div></div>{entries.length ? <div className="full-leaderboard panel">{entries.map((entry, index) => <div className={`full-rank-row ${entry.username === currentUser.username ? 'current' : ''}`} key={entry.username}><b className="big-rank">#{index + 1}</b><div className={`avatar avatar-${['violet', 'blue', 'pink'][index % 3]}`}>{entry.username.slice(0, 2).toUpperCase()}</div><div className="leader-name"><strong>{entry.username}</strong><small>Level {entry.level} · {entry.currentStreak} day streak</small></div><div className="rank-level">LVL {entry.level}</div><strong className="leader-xp">{entry.totalXp.toLocaleString()} <small>XP</small></strong></div>)}</div> : <div className="empty-state">No ranked users yet. Complete a quest to claim the first position.</div>}</div>
}

function AchievementsView({ badges, featuredCodes, onToggleFeatured }) {
  return <div className="feature-view"><div className="feature-heading"><div><p className="eyebrow">YOUR COLLECTION</p><h1>Proof of your <span>progress.</span></h1><p className="muted">Every badge marks a version of you that got stronger.</p></div><div className="challenge-count"><strong>{badges.length}</strong><small>badges earned</small></div></div><div className="featured-hint panel"><span>✦</span><div><strong>Choose your signature badges</strong><small>Pin up to 3 badges to your profile. Selected: {featuredCodes.length}/3</small></div></div><div className="badge-grid">{badges.length ? badges.map((badge) => { const selected = featuredCodes.includes(badge.code); const firstCode = badge.name?.toLowerCase().includes('first code'); return <article className={`badge-card earned ${selected ? 'featured-selected' : ''}`} key={badge.code} role="button" tabIndex="0" onClick={() => onToggleFeatured(badge.code)} onKeyDown={(event) => { if (event.key === 'Enter' || event.key === ' ') onToggleFeatured(badge.code) }}><div className="badge-emblem">{firstCode ? <img src="/first-code-badge.png" alt="" /> : '✦'}</div><div><span>{selected ? 'PINNED TO PROFILE' : 'EARNED'}</span><h3>{badge.name}</h3><p>{badge.description}</p><small>{new Date(badge.awardedAt).toLocaleDateString()}</small></div><b className="badge-pin">{selected ? '✓' : '+'}</b></article> }) : <div className="empty-state">Your first badge is waiting. Complete a challenge to unlock it.</div>}</div></div>
}

function ActivityView({ activity, currentUser }) {
  const activityMap = new Map(activity.map((item) => [item.activityDate, item]))
  const days = Array.from({ length: 35 }, (_, index) => {
    const date = new Date()
    date.setDate(date.getDate() - (34 - index))
    return date.toISOString().slice(0, 10)
  })
  return <div className="feature-view"><div className="feature-heading"><div><p className="eyebrow">YOUR TRAINING LOG</p><h1>Consistency creates <span>mastery.</span></h1><p className="muted">Your recent coding activity and momentum.</p></div><div className="challenge-count"><strong>{currentUser.currentStreak || 0}</strong><small>day streak</small></div></div><div className="activity-summary"><div className="panel activity-stat"><span>PROBLEMS SOLVED</span><strong>{activity.reduce((sum, item) => sum + item.problemsSolved, 0)}</strong></div><div className="panel activity-stat"><span>ACCEPTED RUNS</span><strong>{activity.reduce((sum, item) => sum + item.acceptedSubmissions, 0)}</strong></div><div className="panel activity-stat"><span>XP EARNED</span><strong>{activity.reduce((sum, item) => sum + item.xpEarned, 0)}</strong></div></div><section className="panel activity-panel"><div className="panel-heading"><div><p className="eyebrow">LAST 35 DAYS</p><h3>Activity heatmap</h3></div><span className="heat-legend">Less <i className="heat-0" /><i className="heat-1" /><i className="heat-2" /><i className="heat-3" /> More</span></div><div className="heatmap">{days.map((day) => { const item = activityMap.get(day); const intensity = item ? Math.min(3, Math.max(1, item.problemsSolved)) : 0; return <div className={`heat-cell heat-${intensity}`} title={`${day}: ${item?.problemsSolved || 0} solved`} key={day} /> })}</div></section></div>
}

function RoadmapsView({ onOpenChallenges }) {
  return <div className="feature-view"><div className="feature-heading"><div><p className="eyebrow">STRUCTURED GROWTH</p><h1>Choose your <span>path.</span></h1><p className="muted">Progress through focused skill trees, one quest at a time.</p></div></div><div className="roadmap-grid"><article className="roadmap-feature-card active-path"><div className="roadmap-cover purple-cover">⌁</div><div className="roadmap-feature-copy"><span>PATH 02 · IN PROGRESS</span><h3>Data Structures & Algorithms</h3><p>Build the problem-solving instincts that power every great engineer.</p><div className="path-progress"><i /><small>42% complete · 8 of 19 quests</small></div><button className="solve-button" onClick={onOpenChallenges}>Continue path <span>→</span></button></div></article><article className="roadmap-feature-card"><div className="roadmap-cover cyan-cover">⌘</div><div className="roadmap-feature-copy"><span>PATH 03 · RECOMMENDED</span><h3>Backend Engineering</h3><p>Design APIs, databases and production-ready services.</p><div className="path-meta">12 quests · 560 XP</div><button className="solve-button" onClick={onOpenChallenges}>View path <span>→</span></button></div></article><article className="roadmap-feature-card"><div className="roadmap-cover orange-cover">◈</div><div className="roadmap-feature-copy"><span>PATH 04 · LOCKED</span><h3>System Design</h3><p>Think in scalable systems and architect with confidence.</p><div className="path-meta">Unlock at Level 15</div><button className="solve-button locked" disabled>Locked</button></div></article></div></div>
}

function AdminView({ topics, problems, onDataChanged }) {
  const [topic, setTopic] = useState({ name: '', description: '' })
  const [problem, setProblem] = useState({ title: '', description: '', difficulty: 'EASY', constraints: '', xpReward: 25, topicId: '' })
  const [testCase, setTestCase] = useState({ inputData: '', expectedOutput: '', problemId: '', isHidden: false })
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  const submit = async (request, reset) => {
    setMessage('')
    setError('')
    try {
      await request()
      reset()
      setMessage('Saved successfully.')
      onDataChanged()
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Admin action failed.')
    }
  }

  return (
    <div className="feature-view">
      <div className="feature-heading"><div><p className="eyebrow">ADMIN CONTROL ROOM</p><h1>Manage the <span>learning arena.</span></h1><p className="muted">Create and organize topics, challenges, and evaluation cases from one place.</p></div><button className="primary-button admin-refresh" onClick={onDataChanged}>Refresh data ↻</button></div>
      {message && <p className="settings-message">{message}</p>}
      {error && <p className="auth-error">{error}</p>}
      <div className="admin-summary"><div className="panel admin-stat"><span>TOPICS</span><strong>{topics.length}</strong><small>content paths</small></div><div className="panel admin-stat"><span>PROBLEMS</span><strong>{problems.length}</strong><small>published quests</small></div><div className="panel admin-stat"><span>ACCESS</span><strong>ADMIN</strong><small>protected workspace</small></div></div>
      <div className="admin-grid">
        <section className="panel settings-card"><p className="eyebrow">CONTENT</p><h3>Create topic</h3><form onSubmit={(event) => { event.preventDefault(); submit(() => createTopic(topic), () => setTopic({ name: '', description: '' })) }}><input placeholder="Topic name" value={topic.name} onChange={(event) => setTopic({ ...topic, name: event.target.value })} required /><textarea placeholder="Description" value={topic.description} onChange={(event) => setTopic({ ...topic, description: event.target.value })} /><button className="primary-button">Create topic</button></form></section>
        <section className="panel settings-card"><p className="eyebrow">CONTENT</p><h3>Create problem</h3><form onSubmit={(event) => { event.preventDefault(); submit(() => createProblem({ ...problem, topicId: Number(problem.topicId), xpReward: Number(problem.xpReward) }), () => setProblem({ title: '', description: '', difficulty: 'EASY', constraints: '', xpReward: 25, topicId: '' })) }}><input placeholder="Problem title" value={problem.title} onChange={(event) => setProblem({ ...problem, title: event.target.value })} required /><textarea placeholder="Description" value={problem.description} onChange={(event) => setProblem({ ...problem, description: event.target.value })} required /><select value={problem.difficulty} onChange={(event) => setProblem({ ...problem, difficulty: event.target.value })}><option>EASY</option><option>MEDIUM</option><option>HARD</option></select><input placeholder="XP reward" type="number" min="1" value={problem.xpReward} onChange={(event) => setProblem({ ...problem, xpReward: event.target.value })} required /><select value={problem.topicId} onChange={(event) => setProblem({ ...problem, topicId: event.target.value })} required><option value="">Select topic</option>{topics.map((item) => <option value={item.id} key={item.id}>{item.name}</option>)}</select><textarea placeholder="Constraints (optional)" value={problem.constraints} onChange={(event) => setProblem({ ...problem, constraints: event.target.value })} /><button className="primary-button">Create problem</button></form></section>
        <section className="panel settings-card"><p className="eyebrow">EVALUATION</p><h3>Add test case</h3><form onSubmit={(event) => { event.preventDefault(); submit(() => createTestCase({ ...testCase, problemId: Number(testCase.problemId) }), () => setTestCase({ inputData: '', expectedOutput: '', problemId: '', isHidden: false })) }}><select value={testCase.problemId} onChange={(event) => setTestCase({ ...testCase, problemId: event.target.value })} required><option value="">Select problem</option>{problems.map((item) => <option value={item.id} key={item.id}>{item.title}</option>)}</select><textarea placeholder="Input data" value={testCase.inputData} onChange={(event) => setTestCase({ ...testCase, inputData: event.target.value })} required /><textarea placeholder="Expected output" value={testCase.expectedOutput} onChange={(event) => setTestCase({ ...testCase, expectedOutput: event.target.value })} required /><label className="checkbox-label"><input type="checkbox" checked={testCase.isHidden} onChange={(event) => setTestCase({ ...testCase, isHidden: event.target.checked })} /> Hidden case</label><button className="primary-button">Add test case</button></form></section>
      </div>
      <div className="admin-library">
        <section className="panel admin-list"><div className="panel-heading"><div><p className="eyebrow">CONTENT LIBRARY</p><h3>Topics</h3></div><span className="admin-list-count">{topics.length}</span></div>{topics.length ? topics.map((item) => <div className="admin-list-row" key={item.id}><div><strong>{item.name}</strong><small>{item.description || 'No description added.'}</small></div><b>{problems.filter((problemItem) => problemItem.topic?.id === item.id || problemItem.topicId === item.id).length} problems</b></div>) : <div className="empty-state compact-empty">No topics yet.</div>}</section>
        <section className="panel admin-list"><div className="panel-heading"><div><p className="eyebrow">CONTENT LIBRARY</p><h3>Recent problems</h3></div><span className="admin-list-count">{problems.length}</span></div>{problems.length ? problems.slice(0, 6).map((item) => <div className="admin-list-row" key={item.id}><div><strong>{item.title}</strong><small>Topic #{item.topicId} · +{item.xpReward} XP</small></div><b className={`difficulty ${item.difficulty?.toLowerCase()}`}>{item.difficulty}</b></div>) : <div className="empty-state compact-empty">No problems yet.</div>}</section>
      </div>
    </div>
  )
}

function SettingsView({ currentUser, featuredBadges, onUserUpdated }) {
  const [username, setUsername] = useState(currentUser.username)
  const [profilePhoto, setProfilePhoto] = useState(currentUser.profilePhoto || '')
  const [passwords, setPasswords] = useState({ currentPassword: '', newPassword: '' })
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  const saveProfile = async (event) => {
    event.preventDefault()
    if (username.trim().length < 3) {
      setError('Username must be at least 3 characters.')
      return
    }
    setSaving(true); setMessage(''); setError('')
    try {
      const updated = await updateProfile(username, profilePhoto)
      onUserUpdated({ ...updated, profilePhoto: updated.profilePhoto || profilePhoto })
      setMessage('Profile updated successfully.')
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Could not update your profile.')
    } finally {
      setSaving(false)
    }
  }

  const selectPhoto = (event) => {
    const file = event.target.files?.[0]
    if (!file) return
    if (!file.type.startsWith('image/')) { setError('Please select an image file.'); return }
    if (file.size > 2 * 1024 * 1024) { setError('Profile photo must be smaller than 2 MB.'); return }
    const reader = new FileReader()
    reader.onload = () => { setProfilePhoto(reader.result); setError('') }
    reader.onerror = () => setError('Could not read that image.')
    reader.readAsDataURL(file)
  }

  const removePhoto = () => {
    setProfilePhoto('')
    setMessage('Photo removed. Save your profile to confirm.')
    setError('')
  }

  const savePassword = async (event) => {
    event.preventDefault()
    setSaving(true); setMessage(''); setError('')
    try { await changePassword(passwords.currentPassword, passwords.newPassword); setPasswords({ currentPassword: '', newPassword: '' }); setMessage('Password changed successfully.') } catch (requestError) { setError(requestError.response?.data?.message || 'Could not change your password.') } finally { setSaving(false) }
  }

  return <div className="feature-view"><div className="feature-heading"><div><p className="eyebrow">YOUR ACCOUNT</p><h1>Shape your <span>profile.</span></h1><p className="muted">Keep your account secure and your guild identity current.</p></div></div><section className="profile-preview panel"><div className="profile-cover"><span>YOUR DEVELOPER PROFILE</span></div><div className="profile-preview-body"><UserAvatar user={{ ...currentUser, profilePhoto }} className="avatar avatar-violet avatar-profile" /><div className="profile-identity"><h2>{currentUser.username}</h2><p>Level {currentUser.level || 1} Developer · {currentUser.currentStreak || 0} day streak</p></div><div className="profile-stats"><div><strong>{(currentUser.xp || 0).toLocaleString()}</strong><small>XP</small></div><div><strong>{featuredBadges.length}</strong><small>FEATURED</small></div></div></div><div className="featured-badges"><p className="eyebrow">FEATURED BADGES</p><div>{featuredBadges.map((badge) => <span className="featured-badge" key={badge.code}>✦ {badge.name}</span>)}{!featuredBadges.length && <small className="muted">Choose up to 3 badges from Achievements.</small>}</div></div></section><div className="settings-grid"><section className="panel settings-card"><p className="eyebrow">PUBLIC IDENTITY</p><h3>Profile details</h3><form onSubmit={saveProfile}><div className="photo-picker"><UserAvatar user={{ ...currentUser, profilePhoto }} className="avatar avatar-violet avatar-large" /><div className="photo-actions"><label className="photo-upload">Choose photo<input type="file" accept="image/*" onChange={selectPhoto} /></label>{profilePhoto && <button type="button" className="remove-photo" onClick={removePhoto}>Remove</button>}</div></div><label>Username<input value={username} onChange={(event) => setUsername(event.target.value)} minLength="3" maxLength="50" required /></label><label>Email address<input value={currentUser.email} disabled /></label><button className="primary-button" disabled={saving}>{saving ? 'Saving...' : 'Save profile'}</button></form></section><section className="panel settings-card"><p className="eyebrow">ACCOUNT SECURITY</p><h3>Change password</h3><form onSubmit={savePassword}><label>Current password<input type="password" value={passwords.currentPassword} onChange={(event) => setPasswords({ ...passwords, currentPassword: event.target.value })} required /></label><label>New password<input type="password" value={passwords.newPassword} onChange={(event) => setPasswords({ ...passwords, newPassword: event.target.value })} minLength="8" required /></label><button className="primary-button" disabled={saving}>{saving ? 'Updating...' : 'Update password'}</button></form></section></div>{message && <p className="settings-message">{message}</p>}{error && <p className="auth-error">{error}</p>}</div>
}

function App() {
  const [booting, setBooting] = useState(true)
  const [activeNav, setActiveNav] = useState('Dashboard')
  const [sessionUser, setSessionUser] = useState(null)
  const [leaderboardData, setLeaderboardData] = useState([])
  const [topics, setTopics] = useState([])
  const [problems, setProblems] = useState([])
  const [problemsLoading, setProblemsLoading] = useState(false)
  const [selectedProblem, setSelectedProblem] = useState(null)
  const [badges, setBadges] = useState([])
  const [activity, setActivity] = useState([])
  const [submissions, setSubmissions] = useState([])
  const [authMode, setAuthMode] = useState('login')
  const [user, setUser] = useState({ username: '', email: '', password: '' })
  const [showRegister, setShowRegister] = useState(false)
  const [profileMenuOpen, setProfileMenuOpen] = useState(false)
  const [showFirstBadge, setShowFirstBadge] = useState(false)
  const [featuredBadgeCodes, setFeaturedBadgeCodes] = useState(() => {
    try {
      return JSON.parse(getItem('code2career_featured_badges') || '[]')
    } catch {
      return []
    }
  })

  useEffect(() => {
    const timer = setTimeout(() => setBooting(false), 1800)
    return () => clearTimeout(timer)
  }, [])
  const [message, setMessage] = useState('')

  useEffect(() => {
    const token = getItem(appConfig.tokenStorageKey)
    if (!token) return
    getCurrentUser().then(setSessionUser).catch(() => logout())
  }, [])

  useEffect(() => {
    if (!sessionUser) return
    getLeaderboard().then(setLeaderboardData).catch(() => setLeaderboardData([]))
    getBadges().then(setBadges).catch(() => setBadges([]))
    getSubmissions(sessionUser.id).then(setSubmissions).catch(() => setSubmissions([]))
    const to = new Date()
    const from = new Date()
    from.setDate(to.getDate() - 34)
    getActivity(from.toISOString().slice(0, 10), to.toISOString().slice(0, 10)).then(setActivity).catch(() => setActivity([]))
  }, [sessionUser])

  useEffect(() => {
    if (!sessionUser || !['Challenges', 'Admin'].includes(activeNav)) return
    Promise.all([getTopics(), getProblems()])
      .then(([topicData, problemData]) => {
        setTopics(topicData)
        setProblems(problemData)
      })
      .catch(() => {
        setTopics([])
        setProblems([])
      })
      .finally(() => setProblemsLoading(false))
  }, [activeNav, sessionUser])

  if (booting) {
    return <SplashScreen />
  }

  if (!sessionUser) {
    return <AuthScreen mode={authMode} setMode={setAuthMode} onAuthenticated={setSessionUser} />
  }

  const handleChange = (event) => {
    setUser({ ...user, [event.target.name]: event.target.value })
  }

  const handleRegister = async (event) => {
    event.preventDefault()
    try {
      const registeredUser = await register(user)
      setSessionUser(registeredUser)
      setShowRegister(false)
      setMessage(`Welcome to the guild, ${registeredUser.username}.`)
    } catch {
      setMessage('Registration is unavailable right now. Please try again.')
    }
  }

  const handleLogout = () => {
    logout()
    setSessionUser(null)
    setLeaderboardData([])
    setSelectedProblem(null)
    setBadges([])
    setActivity([])
    setSubmissions([])
  }

  const handleRefresh = async () => {
    try {
      const refreshedUser = await getCurrentUser()
      setSessionUser(refreshedUser)
      setMessage('Profile data refreshed.')
    } catch {
      setMessage('Could not refresh your profile right now.')
    }

  }

  const toggleFeaturedBadge = (badgeCode) => {
    setFeaturedBadgeCodes((current) => {
      const next = current.includes(badgeCode)
        ? current.filter((code) => code !== badgeCode)
        : current.length < 3 ? [...current, badgeCode] : current
      setItem('code2career_featured_badges', JSON.stringify(next))
      return next
    })
  }

  const players = leaderboardData.slice(0, 3).map((player, index) => ({
      rank: String(index + 1).padStart(2, '0'),
      name: player.username,
      handle: `Level ${player.level}`,
      xp: (player.totalXp || 0).toLocaleString(),
      initials: player.username.slice(0, 2).toUpperCase(),
      tone: ['violet', 'blue', 'pink'][index],
    }))
  const acceptedSubmissions = submissions.filter((submission) => submission.status === 'ACCEPTED')
  const featuredBadges = badges.filter((badge) => featuredBadgeCodes.includes(badge.code)).slice(0, 3)
  const today = new Date().toISOString().slice(0, 10)
  const solvedToday = submissions.some((submission) => submission.status === 'ACCEPTED' && submission.submittedAt?.slice(0, 10) === today)
  const completionRate = submissions.length ? Math.round((acceptedSubmissions.length / submissions.length) * 100) : 0
  const dashboardQuests = [
    { title: 'Complete a daily challenge', meta: solvedToday ? 'Complete for today' : 'Not completed today', reward: '+25 XP', progress: solvedToday ? 100 : 0, icon: '⚔' },
    { title: 'Keep your streak alive', meta: `${sessionUser.currentStreak || 0} day streak`, reward: '+15 XP', progress: Math.min(100, ((sessionUser.currentStreak || 0) / 7) * 100), icon: '🔥' },
    { title: 'Build your XP reserve', meta: `${sessionUser.xp || 0} XP earned`, reward: '+40 XP', progress: Math.min(100, ((sessionUser.xp || 0) / 100) * 100), icon: '◈' },
  ]

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <img className="brand-logo" src="/c2c-logo.png" alt="C2C" />
          <div>
            <strong>code<span>2</span>career</strong>
            <small>LEVEL UP YOUR CRAFT</small>
          </div>
        </div>

        <div className="section-label">MAIN MENU</div>
        <nav className="nav-list">
          {[
            ['Dashboard', '⌂'],
            ['Challenges', '◈'],
            ['Roadmaps', '⌁'],
            ['Leaderboard', '♛'],
          ].map(([label, icon]) => (
            <button
              className={`nav-item ${activeNav === label ? 'active' : ''}`}
              key={label}
              onClick={() => {
                if (label === 'Challenges') setProblemsLoading(true)
                setSelectedProblem(null)
                setActiveNav(label)
              }}
            >
              <Icon>{icon}</Icon>
              {label}
              {label === 'Challenges' && <span className="nav-badge">{problems.length || '—'}</span>}
            </button>
          ))}
        </nav>

        <div className="section-label">YOUR PROGRESS</div>
        <nav className="nav-list">
          <button className={`nav-item ${activeNav === 'Activity' ? 'active' : ''}`} onClick={() => setActiveNav('Activity')}><Icon>◷</Icon>Activity</button>
          <button className={`nav-item ${activeNav === 'Achievements' ? 'active' : ''}`} onClick={() => setActiveNav('Achievements')}><Icon>✦</Icon>Achievements</button>
          <button className={`nav-item ${activeNav === 'Settings' ? 'active' : ''}`} onClick={() => setActiveNav('Settings')}><Icon>⚙</Icon>Settings</button>
          {sessionUser.role === 'ADMIN' && <button className={`nav-item ${activeNav === 'Admin' ? 'active' : ''}`} onClick={() => { setSelectedProblem(null); setActiveNav('Admin') }}><Icon>◆</Icon>Admin</button>}
        </nav>

        <div className="sidebar-bottom">
          <div className="upgrade-card">
            <div className="upgrade-orb">✦</div>
            <strong>Unlock your potential</strong>
            <p>Master every path with Pro quests.</p>
            <button className="text-button" onClick={() => setActiveNav('Roadmaps')}>Explore paths <span>→</span></button>
          </div>
          <div className="user-mini">
            <UserAvatar user={sessionUser} />
            <div><strong>{sessionUser.username}</strong><small>Level {sessionUser.level || 1} · Developer</small></div>
            <button className="more-button" aria-label="Open profile menu" onClick={() => setProfileMenuOpen((open) => !open)}>•••</button>
            {profileMenuOpen && <div className="profile-menu"><button onClick={() => { setActiveNav('Settings'); setProfileMenuOpen(false) }}>Profile & settings</button><button onClick={handleLogout}>Log out</button></div>}
          </div>
        </div>
      </aside>

      <main className="main-content">
        <header className="topbar">
          <img className="mobile-brand" src="/c2c-logo.png" alt="C2C" />
          <div className="breadcrumb"><span>Workspace</span><b>/</b>{activeNav}</div>
          <div className="top-actions">
            <button className="icon-button" aria-label="Open challenges" onClick={() => setActiveNav('Challenges')}>⌕</button>
            <button className="icon-button notification" aria-label="Refresh dashboard" onClick={handleRefresh}>♢<i /></button>
            <button className="profile-chip" onClick={() => setActiveNav('Settings')}><UserAvatar user={sessionUser} /><span>{sessionUser.username}</span><b>⌄</b></button>
          </div>
        </header>

        <div className="content-wrap">
          {selectedProblem ? <SolverView key={selectedProblem.id} problem={selectedProblem} onBack={() => { setSelectedProblem(null); setActiveNav('Challenges') }} onSubmissionUpdated={(updated) => setSubmissions((current) => current.some((submission) => submission.id === updated.id) ? current.map((submission) => submission.id === updated.id ? updated : submission) : [updated, ...current])} /> : activeNav === 'Challenges' ? <ChallengesView topics={topics} problems={problems} loading={problemsLoading} onSelectProblem={setSelectedProblem} onTopicChange={(topicId) => { setProblemsLoading(true); getProblems(topicId).then(setProblems).catch(() => setProblems([])).finally(() => setProblemsLoading(false)) }} /> : activeNav === 'Leaderboard' ? <LeaderboardView entries={leaderboardData} currentUser={sessionUser} /> : activeNav === 'Achievements' ? <AchievementsView badges={badges} featuredCodes={featuredBadgeCodes} onToggleFeatured={toggleFeaturedBadge} /> : activeNav === 'Activity' ? <ActivityView activity={activity} currentUser={sessionUser} /> : activeNav === 'Roadmaps' ? <RoadmapsView onOpenChallenges={() => setActiveNav('Challenges')} /> : activeNav === 'Settings' ? <SettingsView currentUser={sessionUser} featuredBadges={featuredBadges} onUserUpdated={setSessionUser} /> : activeNav === 'Admin' && sessionUser.role === 'ADMIN' ? <AdminView topics={topics} problems={problems} onDataChanged={() => { getTopics().then(setTopics); getProblems().then(setProblems) }} /> : <>
          <section className="welcome-row">
            <div>
              <p className="eyebrow">{new Intl.DateTimeFormat('en-US', { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' }).format(new Date()).toUpperCase()}</p>
              <h1>Welcome back, <span>{sessionUser.username}</span> <em>✦</em></h1>
              <p className="muted">Your next level is closer than you think. Keep building.</p>
            </div>
            <button className="primary-button" onClick={() => setActiveNav('Challenges')}><span>＋</span> Start a challenge</button>
          </section>

          <section className="hero-card">
            <div className="hero-copy">
              <div className="status-pill"><i /> CURRENT QUEST</div>
              <h2>Forge your <span>legend.</span></h2>
              <p>Complete challenges, sharpen your skills, and rise through the ranks.</p>
              <button className="hero-button" onClick={() => setActiveNav('Challenges')}>Continue quest <span>→</span></button>
            </div>
            <div className="hero-art" aria-hidden="true">
              <div className="moon" />
              <div className="ring ring-one" /><div className="ring ring-two" />
              <div className="silhouette"><div className="head" /><div className="body" /><div className="sword" /></div>
              <div className="spark spark-one">✦</div><div className="spark spark-two">✧</div>
            </div>
            <div className="hero-level"><span>LEVEL</span><strong>{sessionUser.level || 1}</strong><small>DEVELOPER</small></div>
          </section>

          <button className="first-badge-card panel" onClick={() => setShowFirstBadge(true)} aria-label="Open First Code achievement">
            <img src="/first-code-badge.png" alt="First Code achievement badge" />
            <span><small>ACHIEVEMENT UNLOCKED</small><strong>First Code</strong><em>A small step towards bigger things · Click to inspect</em></span>
            <b>↗</b>
          </button>

          <section className="stat-grid">
            <div className="stat-card"><div className="stat-icon purple">✦</div><div><span>TOTAL XP</span><strong>{(sessionUser.xp || 0).toLocaleString()}</strong><small><b>Live profile</b> current XP</small></div></div>
            <div className="stat-card"><div className="stat-icon orange">🔥</div><div><span>DAY STREAK</span><strong>{sessionUser.currentStreak || 0} days</strong><small><b>Keep going</b> every day</small></div></div>
            <div className="stat-card"><div className="stat-icon blue">♛</div><div><span>GLOBAL RANK</span><strong>{leaderboardData.findIndex((entry) => entry.username === sessionUser.username) >= 0 ? `#${leaderboardData.findIndex((entry) => entry.username === sessionUser.username) + 1}` : '—'}</strong><small><b>Live ranking</b> top 10</small></div></div>
            <div className="stat-card"><div className="stat-icon green">✓</div><div><span>QUESTS CLEARED</span><strong>{acceptedSubmissions.length}</strong><small><b>{completionRate}%</b> completion rate</small></div></div>
          </section>

          <div className="dashboard-grid">
            <section className="panel quests-panel">
              <div className="panel-heading"><div><p className="eyebrow">DAILY OBJECTIVES</p><h3>Active quests</h3></div><button className="link-button" onClick={() => setActiveNav('Challenges')}>View all <span>→</span></button></div>
              <div className="quest-list">
                {dashboardQuests.map((quest) => (
                  <div className="quest-row" key={quest.title}>
                    <div className="quest-icon">{quest.icon}</div>
                    <div className="quest-info"><strong>{quest.title}</strong><small>{quest.meta}</small><div className="progress-track"><i style={{ width: `${quest.progress}%` }} /></div></div>
                    <span className="xp-reward">{quest.reward}</span>
                  </div>
                ))}
              </div>
            </section>

            <section className="panel leaderboard-panel">
              <div className="panel-heading"><div><p className="eyebrow">THE HALL OF FAME</p><h3>Leaderboard</h3></div><button className="link-button" onClick={() => setActiveNav('Leaderboard')}>Full ranking <span>→</span></button></div>
              <div className="leader-list">
                {players.length ? players.map((player) => <div className="leader-row" key={player.rank}><b className="rank">{player.rank}</b><div className={`avatar avatar-${player.tone}`}>{player.initials}</div><div className="leader-name"><strong>{player.name}</strong><small>{player.handle}</small></div><strong className="leader-xp">{player.xp} <small>XP</small></strong></div>) : <div className="empty-state compact-empty">No leaderboard data yet.</div>}
              </div>
              <div className="your-rank"><span>Your current rank</span><strong>{leaderboardData.findIndex((entry) => entry.username === sessionUser.username) >= 0 ? `#${leaderboardData.findIndex((entry) => entry.username === sessionUser.username) + 1}` : 'Not ranked'}</strong><b>{leaderboardData.length ? 'Live' : 'Pending'}</b></div>
            </section>
          </div>

          <section className="panel roadmap-panel">
            <div className="panel-heading"><div><p className="eyebrow">YOUR JOURNEY</p><h3>Continue learning</h3></div><button className="link-button" onClick={() => setActiveNav('Roadmaps')}>Browse roadmaps <span>→</span></button></div>
            <div className="roadmap-list">
              <div className="roadmap-card featured-roadmap"><div className="roadmap-icon">⌁</div><div><span>PATH 02 · IN PROGRESS</span><h4>Data Structures & Algorithms</h4><p>Master the foundations of problem solving.</p><div className="roadmap-progress"><i /><small>42% complete</small></div></div><button className="round-arrow">→</button></div>
              <div className="roadmap-card"><div className="roadmap-icon blue-bg">⌘</div><div><span>PATH 01 · COMPLETED</span><h4>JavaScript Foundations</h4><p>Build your core developer instincts.</p><div className="completed-label">✓ Completed</div></div><button className="round-arrow">→</button></div>
            </div>
          </section>
          </>}
        </div>
      </main>

      {showRegister && <div className="modal-backdrop" onClick={() => setShowRegister(false)}><div className="register-modal" onClick={(event) => event.stopPropagation()}><button className="modal-close" onClick={() => setShowRegister(false)}>×</button><p className="eyebrow">JOIN THE GUILD</p><h2>Create your developer profile</h2><p className="muted">Start your journey and earn your first XP.</p><form onSubmit={handleRegister}><input name="username" placeholder="Username" onChange={handleChange} required /><input name="email" type="email" placeholder="Email address" onChange={handleChange} required /><input name="password" type="password" placeholder="Password (8+ characters)" onChange={handleChange} required minLength="8" /><button className="primary-button" type="submit">Begin your quest <span>→</span></button></form>{message && <p className="form-message">{message}</p>}</div></div>}
      {showFirstBadge && <div className="badge-reveal-backdrop" onClick={() => setShowFirstBadge(false)}><div className="badge-reveal" onClick={(event) => event.stopPropagation()}><button className="modal-close" onClick={() => setShowFirstBadge(false)}>×</button><div className="badge-reveal-glow" /><img src="/first-code-badge.png" alt="First Code achievement badge" /><p className="eyebrow">ACHIEVEMENT UNLOCKED</p><h2>FIRST CODE</h2><p>A small step towards bigger things.</p><button className="primary-button" onClick={() => setShowFirstBadge(false)}>Continue your quest <span>→</span></button></div></div>}
    </div>
  )
}

export default App
