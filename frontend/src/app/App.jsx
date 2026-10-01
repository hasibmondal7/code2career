import { useCallback, useEffect, useRef, useState } from 'react'
import Editor from '@monaco-editor/react'
import { appConfig } from '../config/appConfig'
import { changePassword, createProblem, createPvpRoom, createTestCase, createTopic, getActivity, getBadges, getCurrentUser, getLeaderboard, getOpenPvpRooms, getProblems, getPvpRoom, getRoadmaps, getSubmission, getSubmissions, getTopics, joinPvpRoom, login, logout, register, runCode, startPvpRoom, submitPvpCode, submitSolution, updateProfile } from '../services/api/endpoints'
import { getItem, removeItem, setItem } from '../services/storage/localStorage'
import { subscribeToPvpRoom } from '../services/pvp/pvpSocket'
import { subscribeToSubmission } from '../services/submissions/submissionSocket'

function Icon({ children }) {
  return <span className="icon" aria-hidden="true">{children}</span>
}

function UserAvatar({ user, className = 'avatar avatar-violet' }) {
  return user.profilePhoto
    ? <img className={`${className} avatar-photo`} src={user.profilePhoto} alt={`${user.username} profile`} />
    : <div className={className}>{user.username.slice(0, 2).toUpperCase()}</div>
}

const starterCode = 'import java.util.Scanner;\n\npublic class Solution {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int a = sc.nextInt();\n        int b = sc.nextInt();\n        System.out.println(a + b);\n        sc.close();\n    }\n}'
const starterTemplates = {
  java: starterCode,
  cpp: '#include <iostream>\nusing namespace std;\n\nint main() {\n    // Write your solution here\n    return 0;\n}',
  python: 'def solve():\n    # Write your solution here\n    pass\n\nif __name__ == "__main__":\n    solve()',
  javascript: 'function solve() {\n  // Write your solution here\n}\n\nsolve();',
}
const pvpTemplates = {
  twoSum: {
    JAVA: `import java.util.*;
import java.util.regex.*;

class Solution {
    public static void main(String[] args) throws Exception {
        String input = new String(System.in.readAllBytes());
        Matcher matcher = Pattern.compile("-?\\\\d+").matcher(input);
        List<Integer> values = new ArrayList<>();
        while (matcher.find()) values.add(Integer.parseInt(matcher.group()));
        int[] nums = values.subList(0, values.size() - 1).stream().mapToInt(Integer::intValue).toArray();
        int target = values.get(values.size() - 1);
        int[] answer = twoSum(nums, target);
        System.out.print("[" + answer[0] + "," + answer[1] + "]");
    }

    static int[] twoSum(int[] nums, int target) {
        // TODO: return the indices of the two numbers that add up to target.
        return new int[0];
    }
}`,
    CPP: `#include <iostream>
#include <regex>
#include <string>
#include <vector>
using namespace std;

vector<int> twoSum(const vector<int>& nums, int target) {
    // TODO: return the indices of the two numbers that add up to target.
    return {};
}

int main() {
    string input((istreambuf_iterator<char>(cin)), istreambuf_iterator<char>());
    regex number("-?\\\\d+");
    vector<int> values;
    for (sregex_iterator it(input.begin(), input.end(), number), end; it != end; ++it)
        values.push_back(stoi(it->str()));
    vector<int> nums(values.begin(), values.end() - 1);
    vector<int> answer = twoSum(nums, values.back());
    cout << "[" << answer[0] << "," << answer[1] << "]";
}`,
    PYTHON: `import re

def two_sum(nums, target):
    # TODO: return the indices of the two numbers that add up to target.
    return []

values = list(map(int, re.findall(r"-?\\d+", open(0).read())))
answer = two_sum(values[:-1], values[-1])
print(f"[{answer[0]},{answer[1]}]")`,
    JAVASCRIPT: `const input = require('fs').readFileSync(0, 'utf8');
const values = input.match(/-?\\d+/g).map(Number);

function twoSum(nums, target) {
  // TODO: return the indices of the two numbers that add up to target.
  return [];
}

const answer = twoSum(values.slice(0, -1), values.at(-1));
process.stdout.write(\`[\${answer[0]},\${answer[1]}]\`);`,
  },
  palindrome: {
    JAVA: `import java.util.*;

class Solution {
    public static void main(String[] args) {
        int number = new Scanner(System.in).nextInt();
        System.out.print(isPalindrome(number));
    }

    static boolean isPalindrome(int number) {
        // TODO: return whether number reads the same forwards and backwards.
        return false;
    }
}`,
    CPP: `#include <iostream>
using namespace std;

bool isPalindrome(int number) {
    // TODO: return whether number reads the same forwards and backwards.
    return false;
}

int main() {
    int number;
    cin >> number;
    cout << (isPalindrome(number) ? "true" : "false");
}`,
    PYTHON: `def is_palindrome(number):
    # TODO: return whether number reads the same forwards and backwards.
    return False

number = int(input())
print(str(is_palindrome(number)).lower())`,
    JAVASCRIPT: `const number = Number(require('fs').readFileSync(0, 'utf8').trim());

function isPalindrome(value) {
  // TODO: return whether value reads the same forwards and backwards.
  return false;
}

process.stdout.write(String(isPalindrome(number)));`,
  },
}

function getPvpStarterCode(problemTitle, language, templates = {}) {
  const normalizedTitle = problemTitle.toLowerCase()
  const templatesForProblem = normalizedTitle.includes('two sum')
    ? pvpTemplates.twoSum
    : normalizedTitle.includes('palindrome')
      ? pvpTemplates.palindrome
      : null
  return templatesForProblem?.[language] || templates[language] || starterTemplates[language.toLowerCase()] || starterCode
}

const languageOptions = [
  { value: 'java', label: 'Java 17', file: 'Solution.java' },
  { value: 'cpp', label: 'C++', file: 'Solution.cpp' },
  { value: 'python', label: 'Python 3', file: 'solution.py' },
  { value: 'javascript', label: 'JavaScript', file: 'solution.js' },
]
function compressImageFile(file, { maxWidth, maxHeight, quality }) {
  return new Promise((resolve, reject) => {
    const objectUrl = URL.createObjectURL(file)
    const image = new Image()
    image.onload = () => {
      const scale = Math.min(1, maxWidth / image.width, maxHeight / image.height)
      const width = Math.max(1, Math.round(image.width * scale))
      const height = Math.max(1, Math.round(image.height * scale))
      const canvas = document.createElement('canvas')
      canvas.width = width
      canvas.height = height
      const context = canvas.getContext('2d')
      if (!context) {
        URL.revokeObjectURL(objectUrl)
        reject(new Error('Could not prepare image preview.'))
        return
      }
      context.drawImage(image, 0, 0, width, height)
      const dataUrl = canvas.toDataURL('image/jpeg', quality)
      URL.revokeObjectURL(objectUrl)
      resolve(dataUrl)
    }
    image.onerror = () => {
      URL.revokeObjectURL(objectUrl)
      reject(new Error('Could not read that image.'))
    }
    image.src = objectUrl
  })
}
function toLocalDateKey(value = new Date()) {
  if (typeof value === 'string' && value.trim()) return value.slice(0, 10)
  if (Array.isArray(value) && value.length >= 3) {
    return `${value[0]}-${String(value[1]).padStart(2, '0')}-${String(value[2]).padStart(2, '0')}`
  }
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return String(value || "").slice(0, 10)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const preferenceDefaults = { theme: 'dark', fontSize: 'medium', language: 'en' }
const shellLabels = {
  en: { Dashboard: 'Dashboard', Challenges: 'Challenges', PvP: 'PvP Arena', Roadmaps: 'Roadmaps', Leaderboard: 'Leaderboard', Profile: 'Profile', Activity: 'Activity', Achievements: 'Achievements', Settings: 'Settings', Admin: 'Admin', main: 'MAIN MENU', progress: 'YOUR PROGRESS', workspace: 'Workspace', logout: 'Log out' },
  bn: { Dashboard: 'ড্যাশবোর্ড', Challenges: 'চ্যালেঞ্জ', PvP: 'পিভিপি', Roadmaps: 'রোডম্যাপ', Leaderboard: 'লিডারবোর্ড', Profile: 'প্রোফাইল', Activity: 'অ্যাক্টিভিটি', Achievements: 'অর্জন', Settings: 'সেটিংস', Admin: 'অ্যাডমিন', main: 'প্রধান মেনু', progress: 'আপনার অগ্রগতি', workspace: 'ওয়ার্কস্পেস', logout: 'লগ আউট' },
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
    const [language, setLanguage] = useState('java')
    const [code, setCode] = useState(() => {
      const draft = getItem(`${appConfig.draftStoragePrefix}${problem.id}_java`) || getItem(`${appConfig.draftStoragePrefix}${problem.id}`)
      return draft === starterCode ? '' : draft || ''
    })
    const [customInput, setCustomInput] = useState('')
    const [output, setOutput] = useState('')
    const [status, setStatus] = useState('')
    const [running, setRunning] = useState(false)

    useEffect(() => {
      setItem(`${appConfig.draftStoragePrefix}${problem.id}_${language}`, code)
    }, [code, language, problem.id])

    const handleLanguageChange = (nextLanguage) => {
      if (nextLanguage === language) return
      if (code.trim() && !window.confirm('Changing languages will replace the current editor contents with a starter template. Continue?')) return
      setLanguage(nextLanguage)
      const savedDraft = getItem(`${appConfig.draftStoragePrefix}${problem.id}_${nextLanguage}`)
      setCode(savedDraft || starterTemplates[nextLanguage])
      setOutput('')
      setStatus('')
    }

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
        if (!code.trim()) {
        setOutput('Write your Java solution before running it.')
        setStatus('Code is required')
        return
      }
      setRunning(true)
      setStatus('Running code...')
      try {
        const normalizedInput = customInput.replace(/,\s*/g, '\n')
        const result = await runCode(code, normalizedInput, language)
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
      if (!code.trim()) {
        setOutput('Write your Java solution before submitting it.')
        setStatus('Code is required')
        return
      }
      setRunning(true)
      setStatus('Submitting for evaluation...')
      let disconnect
      let finished = false
      let submittedResult

      try {
        submittedResult = await submitSolution(problem.id, code, language)
        const result = submittedResult
        setOutput(`Submission #${result.id} is ${result.status}. Your result will appear in activity.`)

        const applySubmissionUpdate = async () => {
          const updated = await getSubmission(result.id)
          onSubmissionUpdated(updated)
          if (updated.status !== 'PENDING') {
            finished = true
            setOutput(`Submission #${updated.id}: ${updated.status}`)
            setStatus(updated.status === 'ACCEPTED' ? 'Quest cleared' : 'Try again')
            setRunning(false)
            if (disconnect) disconnect()
          }
        }

        disconnect = await subscribeToSubmission(result.id, applySubmissionUpdate)
        setStatus('Waiting for evaluation...')

        // Cover the small window where evaluation finishes before the subscription is active.
        await applySubmissionUpdate()
        if (finished) return

        await new Promise((resolve) => setTimeout(resolve, 15_000))
        if (!finished) {
          throw new Error('Live evaluation update timed out')
        }
      } catch (error) {
        if (disconnect) disconnect()

        if (submittedResult && !finished) {
          for (let attempt = 0; attempt < 6; attempt += 1) {
            await new Promise((resolve) => setTimeout(resolve, 1500))
            const updated = await getSubmission(submittedResult.id)
            onSubmissionUpdated(updated)
            if (updated.status !== 'PENDING') {
              finished = true
              setOutput(`Submission #${updated.id}: ${updated.status}`)
              setStatus(updated.status === 'ACCEPTED' ? 'Quest cleared' : 'Try again')
              break
            }
          }
        }

        if (!finished) {
          setOutput(error.response?.data?.message || 'Submission failed. Please try again.')
          setStatus(submittedResult ? 'Evaluation delayed' : 'Submission failed')
        }
      } finally {
        setRunning(false)
      }
    }

    return (
      <div className="solver-view">
        <button className="back-link" onClick={onBack}>← Back to challenges</button>
        <div className="solver-layout">
          <section className="problem-description panel"><div className="challenge-tags"><span className={`difficulty ${problem.difficulty.toLowerCase()}`}>{problem.difficulty}</span><span>+{problem.xpReward} XP</span></div><h1>{problem.title}</h1><p>{problem.description}</p>{problem.constraints && <><h4>Constraints</h4><p className="constraints">{problem.constraints}</p></>}<div className="solver-tip"><span>✦</span><div><strong>Quest tip</strong><p>Test your idea with custom input before submitting.</p></div></div></section>
          <section className="editor-panel panel"><div className="editor-toolbar"><span><i /> {languageOptions.find((option) => option.value === language)?.file}</span><select value={language} onChange={(event) => handleLanguageChange(event.target.value)} aria-label="Select coding language">{languageOptions.map((option) => <option value={option.value} key={option.value}>{option.label}</option>)}</select></div><Editor height="360px" language={language} theme="vs-dark" value={code} beforeMount={configureJavaEditor} onChange={(value) => setCode(value || '')} options={{ automaticLayout: true, minimap: { enabled: false }, fontSize: 13, lineNumbers: 'on', tabSize: 4, suggestOnTriggerCharacters: language === 'java', quickSuggestions: language === 'java', padding: { top: 16 } }} /><div className="language-note">Java 17, C++17, Python 3, and JavaScript runtimes available</div><div className="input-label">CUSTOM INPUT</div><textarea className="custom-input" placeholder={'Example: 15\n25 (commas also supported)'} value={customInput} onChange={(event) => setCustomInput(event.target.value)} /><div className="editor-actions"><span className="run-status">{status}</span><button className="run-button" onClick={handleRun} disabled={running}>▶ Run code</button><button className="submit-button" onClick={handleSubmit} disabled={running}>Submit solution <span>→</span></button></div>{output && <pre className="output-panel">{output}</pre>}</section>
        </div>
      </div>
    )
}

function LeaderboardView({ entries, currentUser }) {
  return <div className="feature-view"><div className="feature-heading"><div><p className="eyebrow">THE HALL OF FAME</p><h1>Rise through the <span>ranks.</span></h1><p className="muted">Every solved problem moves you closer to the top.</p></div><div className="challenge-count"><strong>TOP 10</strong><small>global ranking</small></div></div>{entries.length ? <div className="full-leaderboard panel">{entries.map((entry, index) => <div className={`full-rank-row ${entry.username === currentUser.username ? 'current' : ''}`} key={entry.username}><b className="big-rank">#{index + 1}</b><UserAvatar user={entry} className={`avatar avatar-${['violet', 'blue', 'pink'][index % 3]}`} /><div className="leader-name"><strong>{entry.username}</strong><small>Level {entry.level} · {entry.currentStreak} day streak</small></div><div className="rank-level">LVL {entry.level}</div><strong className="leader-xp">{entry.totalXp.toLocaleString()} <small>XP</small></strong></div>)}</div> : <div className="empty-state">No ranked users yet. Complete a quest to claim the first position.</div>}</div>
}

function AchievementsView({ badges, featuredCodes, onToggleFeatured, currentUser, submissions }) {
  const solvedCount = submissions.filter((submission) => submission.status === 'ACCEPTED').length
  const missions = [
    { code: 'FIRST_CODE', name: 'First Code', description: 'Submit your first accepted solution.', current: solvedCount, target: 1 },
    { code: 'TEN_PROBLEMS', name: 'Ten Problems', description: 'Solve ten coding problems.', current: solvedCount, target: 10 },
    { code: 'HUNDRED_XP', name: 'Hundred XP', description: 'Earn 100 XP from accepted solutions.', current: currentUser.xp || 0, target: 100 },
    { code: 'SEVEN_DAY_STREAK', name: '7 Day Streak', description: 'Code for seven consecutive days.', current: currentUser.currentStreak || 0, target: 7 },
  ]
  const earnedCodes = new Set(badges.map((badge) => badge.code))
  return <div className="feature-view"><div className="feature-heading"><div><p className="eyebrow">YOUR COLLECTION</p><h1>Proof of your <span>progress.</span></h1><p className="muted">Complete missions to unlock badges and pin up to three on your profile.</p></div><div className="challenge-count"><strong>{badges.length}</strong><small>badges earned</small></div></div><div className="featured-hint panel"><span>✦</span><div><strong>Choose your signature badges</strong><small>Pin up to 3 earned badges. Selected: {featuredCodes.length}/3</small></div></div><div className="badge-grid">{missions.map((mission) => { const badge = badges.find((item) => item.code === mission.code); const earned = earnedCodes.has(mission.code); const selected = featuredCodes.includes(mission.code); const progress = Math.min(100, Math.round((mission.current / mission.target) * 100)); return <article className={`badge-card ${earned ? `earned ${selected ? 'featured-selected' : ''}` : 'locked-mission'}`} key={mission.code} role={earned ? 'button' : undefined} tabIndex={earned ? 0 : undefined} onClick={earned ? () => onToggleFeatured(mission.code) : undefined} onKeyDown={earned ? (event) => { if (event.key === 'Enter' || event.key === ' ') onToggleFeatured(mission.code) } : undefined}><div className="badge-emblem">{earned && mission.code === 'FIRST_CODE' ? <img src="/first-code-badge.png" alt="" /> : earned ? '✦' : '🔒'}</div><div><span>{earned ? (selected ? 'PINNED TO PROFILE' : 'EARNED') : 'MISSION'}</span><h3>{badge?.name || mission.name}</h3><p>{badge?.description || mission.description}</p>{earned ? <small>{new Date(badge.awardedAt).toLocaleDateString()}</small> : <><div className="mission-progress"><i style={{ width: `${progress}%` }} /></div><small>{mission.current}/{mission.target} completed</small></>}</div>{earned && <b className="badge-pin">{selected ? '✓' : '+'}</b>}</article> })}</div></div>
}

function ActivityView({ activity, currentUser, loading, error, onRetry }) {
  const activityMap = new Map(activity.map((item) => [toLocalDateKey(item.activityDate), item]))
  const days = Array.from({ length: 35 }, (_, index) => {
    const date = new Date()
    date.setDate(date.getDate() - (34 - index))
    return toLocalDateKey(date)
  })
  return <div className="feature-view"><div className="feature-heading"><div><p className="eyebrow">YOUR TRAINING LOG</p><h1>Consistency creates <span>mastery.</span></h1><p className="muted">Your recent coding activity and momentum.</p></div><div className="challenge-count"><strong>{currentUser.currentStreak || 0}</strong><small>day streak</small></div></div>{loading ? <div className="empty-state activity-loading">Loading your activity...</div> : error ? <div className="empty-state activity-error"><p>{error}</p><button className="primary-button" onClick={onRetry}>Retry activity</button></div> : <><div className="activity-summary"><div className="panel activity-stat"><span>PROBLEMS SOLVED</span><strong>{activity.reduce((sum, item) => sum + item.problemsSolved, 0)}</strong></div><div className="panel activity-stat"><span>ACCEPTED RUNS</span><strong>{activity.reduce((sum, item) => sum + item.acceptedSubmissions, 0)}</strong></div><div className="panel activity-stat"><span>XP EARNED</span><strong>{activity.reduce((sum, item) => sum + item.xpEarned, 0)}</strong></div></div><section className="panel activity-panel"><div className="panel-heading"><div><p className="eyebrow">LAST 35 DAYS</p><h3>Activity heatmap</h3></div><span className="heat-legend">Less <i className="heat-0" /><i className="heat-1" /><i className="heat-2" /><i className="heat-3" /> More</span></div><div className="heatmap">{days.map((day) => { const item = activityMap.get(day); const intensity = item ? Math.min(3, Math.max(1, item.problemsSolved)) : 0; return <div className={`heat-cell heat-${intensity}`} title={`${day}: ${item?.problemsSolved || 0} solved`} key={day} /> })}</div></section></>}</div>
}

function RoadmapsView({ roadmaps, loading, error, onOpenChallenges }) {
  return <div className="feature-view"><div className="feature-heading"><div><p className="eyebrow">STRUCTURED GROWTH</p><h1>Choose your <span>path.</span></h1><p className="muted">Progress through focused skill trees, one quest at a time.</p></div></div>{loading ? <div className="empty-state">Loading your roadmaps...</div> : error ? <div className="empty-state">{error}</div> : !roadmaps.length ? <div className="empty-state">No roadmaps are available yet.</div> : <div className="roadmap-grid">{roadmaps.map((roadmap) => { const status = roadmap.progressPercent >= 100 ? 'COMPLETED' : roadmap.progressPercent > 0 ? 'IN PROGRESS' : 'RECOMMENDED'; const coverClass = `${roadmap.accent || 'purple'}-cover`; return <article className={`roadmap-feature-card ${status === 'IN PROGRESS' ? 'active-path' : ''}`} key={roadmap.id}><div className={`roadmap-cover ${coverClass}`}>{roadmap.icon}</div><div className="roadmap-feature-copy"><span>{roadmap.title.toUpperCase()} · {status}</span><h3>{roadmap.title}</h3><p>{roadmap.description}</p>{status === 'COMPLETED' ? <div className="path-meta">✓ Completed · {roadmap.totalSteps} quests</div> : <div className="path-progress"><i style={{ width: `${roadmap.progressPercent}%` }} /><small>{roadmap.progressPercent}% complete · {roadmap.completedSteps} of {roadmap.totalSteps} quests</small></div>}<button className="solve-button" onClick={onOpenChallenges}>{status === 'IN PROGRESS' ? 'Continue path' : status === 'COMPLETED' ? 'Review path' : 'Start path'} <span>→</span></button></div></article> })}</div>}</div>
}

function PvpView({ problems, problemsLoading, currentUser, onUserUpdate }) {
  const savedRoomKey = `code2career_pvp_room_${currentUser.id}`
  const [roomName, setRoomName] = useState('')
  const [problemId, setProblemId] = useState('')
  const [difficulty, setDifficulty] = useState('EASY')
  const [language, setLanguage] = useState('JAVA')
  const [timeLimitMinutes, setTimeLimitMinutes] = useState(15)
  const [stakeCoins, setStakeCoins] = useState(10)
  const [publicMatch, setPublicMatch] = useState(false)
  const [joinCode, setJoinCode] = useState('')
  const [room, setRoom] = useState(null)
  const initializedRoomCode = useRef(null)
  const [openRooms, setOpenRooms] = useState([])
  const [code, setCode] = useState(starterCode)
  const [submissionResult, setSubmissionResult] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [roomConnection, setRoomConnection] = useState('LIVE')
  const [now, setNow] = useState(0)
  const availableProblems = problems.filter((problem) => problem.difficulty === difficulty)
  const currentPlayer = room?.players.find((player) => player.userId === currentUser.id)
  const remainingSeconds = now && room?.endsAt
    ? Math.max(0, Math.ceil((new Date(room.endsAt).getTime() - now) / 1000))
    : null
  const setActiveRoom = useCallback((activeRoom) => {
    if (initializedRoomCode.current !== activeRoom.inviteCode) {
      const selectedProblem = problems.find((problem) => problem.id === activeRoom.problemId)
      setCode(getPvpStarterCode(activeRoom.problemTitle, activeRoom.language, selectedProblem?.templates))
      initializedRoomCode.current = activeRoom.inviteCode
    }
    setRoom(activeRoom)
  }, [problems])

  useEffect(() => {
    const savedInviteCode = getItem(savedRoomKey)
    if (!savedInviteCode) return
    getPvpRoom(savedInviteCode)
      .then(setActiveRoom)
      .catch((requestError) => {
        removeItem(savedRoomKey)
        setError(requestError.response?.data?.message || 'Your saved PvP room is no longer available.')
      })
  }, [savedRoomKey, setActiveRoom])

  useEffect(() => {
    if (room) return undefined
    let active = true
    const refresh = () => getOpenPvpRooms().then((data) => {
      if (active) setOpenRooms(data)
    }).catch((requestError) => {
      if (active) setError(requestError.response?.data?.message || 'Could not load online matches.')
    })
    refresh()
    const interval = window.setInterval(refresh, 5000)
    return () => {
      active = false
      window.clearInterval(interval)
    }
  }, [room])

  useEffect(() => {
    if (!room?.inviteCode) return undefined
    let active = true
    let unsubscribe
    subscribeToPvpRoom(
      room.inviteCode,
      (updatedRoom) => {
        if (active) {
          setActiveRoom(updatedRoom)
          getCurrentUser().then(onUserUpdate).catch((requestError) => {
            if (active) setError(requestError.response?.data?.message || 'Could not refresh your coin balance.')
          })
        }
      },
      () => {
        if (active) setRoomConnection('POLLING')
      },
    ).then((cleanup) => {
      if (active) {
        unsubscribe = cleanup
        setRoomConnection('LIVE')
      }
      else cleanup()
    }).catch(() => {
      if (active) setRoomConnection('POLLING')
    })
    return () => {
      active = false
      unsubscribe?.()
    }
  }, [room?.inviteCode, onUserUpdate, setActiveRoom])

  useEffect(() => {
    if (!room?.inviteCode) return undefined
    let active = true
    const refreshRoom = () => {
      getPvpRoom(room.inviteCode).then((updatedRoom) => {
        if (!active) return
        setActiveRoom(updatedRoom)
        const updatedPlayer = updatedRoom.players.find((player) => player.userId === currentUser.id)
        if (updatedPlayer && updatedPlayer.coins !== currentUser.coins) {
          onUserUpdate({ ...currentUser, coins: updatedPlayer.coins })
        }
      }).catch((requestError) => {
        if (active) setError(requestError.response?.data?.message || 'Could not refresh the battle room.')
      })
    }
    const interval = window.setInterval(refreshRoom, 3000)
    return () => {
      active = false
      window.clearInterval(interval)
    }
  }, [room?.inviteCode, currentUser, onUserUpdate, setActiveRoom])

  useEffect(() => {
    if (!room || room.status !== 'IN_PROGRESS') return undefined
    const interval = window.setInterval(() => setNow(Date.now()), 1000)
    return () => window.clearInterval(interval)
  }, [room])

  useEffect(() => {
    if (!room || room.status !== 'IN_PROGRESS' || remainingSeconds !== 0) return
    getPvpRoom(room.inviteCode).then(setActiveRoom).catch((requestError) => {
      setError(requestError.response?.data?.message || 'Could not finish the expired battle.')
    })
  }, [room, remainingSeconds, setActiveRoom])

  const performRoomAction = async (action) => {
    setBusy(true)
    setError('')
    try {
      const updatedRoom = await action()
      setItem(savedRoomKey, updatedRoom.inviteCode)
      setActiveRoom(updatedRoom)
      setSubmissionResult(null)
    } catch (requestError) {
      setError(requestError.response?.data?.message || requestError.message || 'PvP room request failed.')
    } finally {
      setBusy(false)
    }
  }

  const handleCreate = (event) => {
    event.preventDefault()
    if (!problemId) {
      setError('Choose a problem for this battle.')
      return
    }
    performRoomAction(() => createPvpRoom({
      roomName: roomName.trim() || `${currentUser.username}'s battle`,
      problemId: Number(problemId),
      timeLimitMinutes: Number(timeLimitMinutes),
      maxPlayers: 2,
      stakeCoins: Number(stakeCoins),
      language,
      publicMatch,
    }))
  }

  const handleJoin = (event) => {
    event.preventDefault()
    if (!joinCode.trim()) {
      setError('Enter a room invite code.')
      return
    }
    performRoomAction(() => joinPvpRoom(joinCode.trim()))
  }

  const handleStart = () => performRoomAction(() => startPvpRoom(room.inviteCode))
  const handleRefreshRoom = () => performRoomAction(() => getPvpRoom(room.inviteCode))
  const handleSubmit = async (event) => {
    event.preventDefault()
    if (!code.trim()) return setError('Write your Java solution before submitting.')
    setBusy(true)
    setError('')
    setSubmissionResult(null)
    try {
      const result = await submitPvpCode(room.inviteCode, code, room.language)
      setSubmissionResult(result)
      if (result.winner) onUserUpdate(await getCurrentUser())
      setActiveRoom(await getPvpRoom(room.inviteCode))
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Could not submit your battle solution.')
    } finally {
      setBusy(false)
    }
  }
  const copyInviteCode = async () => {
    try {
      await navigator.clipboard.writeText(room.inviteCode)
    } catch {
      setError('Could not copy automatically. Select and copy the invite code.')
    }
  }
  const isHost = room?.hostId === currentUser.id
  const winner = room?.players.find((player) => player.userId === room.winnerId)

  return (
    <div className="feature-view">
      <div className="feature-heading">
        <div><p className="eyebrow">REAL-TIME COMPETITION</p><h1>Enter the <span>PvP arena.</span></h1><p className="muted">Create a private room, invite a friend, then race to solve the same problem.</p></div>
      </div>
      {error && <p className="auth-error" role="alert">{error}</p>}
      {room ? (
        <section className="panel settings-card">
          <p className="eyebrow">{room.status === 'WAITING' ? 'WAITING LOBBY' : room.status === 'FINISHED' ? 'BATTLE COMPLETE' : 'BATTLE IN PROGRESS'}</p>
          <h2>{room.roomName}</h2>
          <p className="muted">Invite code: <strong>{room.inviteCode}</strong> <button className="text-button" type="button" onClick={copyInviteCode}>Copy invite</button></p>
          <p><strong>{room.problemTitle}</strong> · {room.problemDifficulty} · {room.timeLimitMinutes} minutes · {room.language}</p>
          {roomConnection === 'POLLING' && <p className="muted">Live connection unavailable; room status refreshes automatically.</p>}
          <p>Entry: <strong>{room.stakeCoins} coins</strong> · Winner takes {room.stakeCoins * room.players.length} coins · Your balance: {currentPlayer?.coins ?? currentUser.coins ?? 0}</p>
          <h3>Players ({room.players.length}/{room.maxPlayers})</h3>
          <div className="room-player-list">{room.players.map((player) => <div className="panel room-player" key={player.userId}><span>{player.username}</span>{player.host && <small>HOST</small>}</div>)}</div>
          {room.status === 'WAITING' && isHost && <button className="primary-button" disabled={busy || room.players.length < 2} onClick={handleStart}>{busy ? 'Starting...' : room.players.length < 2 ? 'Waiting for an opponent' : 'Start battle'}</button>}
          {room.status === 'IN_PROGRESS' && <><p className="eyebrow">TIME LEFT · {remainingSeconds === null ? '--:--' : `${Math.floor(remainingSeconds / 60)}:${String(remainingSeconds % 60).padStart(2, '0')}`}</p><form onSubmit={handleSubmit}><Editor height="340px" language={room.language.toLowerCase() === 'cpp' ? 'cpp' : room.language.toLowerCase() === 'python' ? 'python' : room.language.toLowerCase() === 'javascript' ? 'javascript' : 'java'} theme="vs-dark" value={code} onChange={(value) => setCode(value || '')} options={{ automaticLayout: true, minimap: { enabled: false }, fontSize: 14, padding: { top: 14 } }} /><button className="primary-button" type="submit" disabled={busy || remainingSeconds === 0}>{busy ? 'Evaluating...' : 'Submit battle solution'}</button></form></>}
          {room.status === 'FINISHED' && <p className="muted">{winner ? `${winner.username} won the battle and earned ${room.stakeCoins * room.players.length} coins + ${room.winnerXp} XP.` : 'Time expired with no winner. Entry coins were returned.'}</p>}
          {submissionResult && <p className={submissionResult.winner ? 'settings-message' : submissionResult.status === 'SYSTEM_ERROR' ? 'auth-error' : 'muted'}>{submissionResult.winner ? `Accepted! +${submissionResult.coinsWon} coins · +${submissionResult.xpWon} XP` : submissionResult.status === 'SYSTEM_ERROR' ? 'Code runner is unavailable. Restart the backend and code-runner containers, then try again.' : `Submission result: ${submissionResult.status}`}</p>}
          <button className="text-button" disabled={busy} onClick={handleRefreshRoom}>Refresh room</button>
          <button className="text-button" onClick={() => { removeItem(savedRoomKey); initializedRoomCode.current = null; setRoom(null); setError('') }}>Close room view</button>
        </section>
      ) : (
        <div className="settings-grid">
          <section className="panel settings-card">
            <p className="eyebrow">HOST A PRIVATE BATTLE</p><h3>Create a room</h3>
            <form onSubmit={handleCreate}>
              <label>Room name<input maxLength="80" value={roomName} onChange={(event) => setRoomName(event.target.value)} placeholder={`${currentUser.username}'s battle`} /></label>
              <label>Difficulty<select value={difficulty} onChange={(event) => { setDifficulty(event.target.value); setProblemId('') }}>{['EASY', 'MEDIUM', 'HARD'].map((level) => <option key={level}>{level}</option>)}</select></label>
              <label>Problem<select value={problemId} onChange={(event) => setProblemId(event.target.value)} required disabled={problemsLoading}><option value="">Choose a problem</option>{availableProblems.map((problem) => <option key={problem.id} value={problem.id}>{problem.title}</option>)}</select></label>
              <label>Language<select value={language} onChange={(event) => setLanguage(event.target.value)} aria-label="Battle language"><option value="JAVA">Java 17</option><option value="CPP">C++17</option><option value="PYTHON">Python 3</option><option value="JAVASCRIPT">JavaScript</option></select></label>
              <label>Time limit<select value={timeLimitMinutes} onChange={(event) => setTimeLimitMinutes(event.target.value)}>{[5, 10, 15, 20, 30, 45, 60].map((minutes) => <option key={minutes} value={minutes}>{minutes} minutes</option>)}</select></label>
              <label>Entry coins<select value={stakeCoins} onChange={(event) => setStakeCoins(event.target.value)}>{[10, 25, 50, 100].map((coins) => <option key={coins} value={coins}>{coins} coins</option>)}</select></label>
              <label><input type="checkbox" checked={publicMatch} onChange={(event) => setPublicMatch(event.target.checked)} /> List in online matchmaking</label>
              <button className="primary-button" disabled={busy || problemsLoading || !availableProblems.length || (currentUser.coins ?? 100) < Number(stakeCoins)}>{busy ? 'Creating...' : 'Create battle room'}</button>
              {(currentUser.coins ?? 100) < Number(stakeCoins) && <small>You need at least {stakeCoins} coins to enter this battle.</small>}
            </form>
          </section>
          <section className="panel settings-card">
            <p className="eyebrow">JOIN YOUR FRIEND</p><h3>Have an invite code?</h3>
            <form onSubmit={handleJoin}><label>Room invite code<input value={joinCode} onChange={(event) => setJoinCode(event.target.value.toUpperCase())} maxLength="12" autoComplete="off" placeholder="e.g. ABCD2345" required /></label><button className="primary-button" disabled={busy}>{busy ? 'Joining...' : 'Join room'}</button></form>
          </section>
          <section className="panel settings-card">
            <p className="eyebrow">ONLINE MATCHMAKING</p><h3>Accept an open battle</h3>
            {openRooms.length ? openRooms.map((openRoom) => <div className="room-player" key={openRoom.inviteCode}><span>{openRoom.roomName} · {openRoom.problemDifficulty} · {openRoom.stakeCoins} coins</span><button className="solve-button" disabled={busy || (currentUser.coins ?? 100) < openRoom.stakeCoins} onClick={() => performRoomAction(() => joinPvpRoom(openRoom.inviteCode))}>Accept</button></div>) : <p className="muted">No open matches now. Create a public room and wait for an opponent.</p>}
          </section>
        </div>
      )}
    </div>
  )
}

function ProfileView({ currentUser, badges, featuredBadges, submissions, activity, leaderboardData, problems, onUserUpdated, onBadgeClick }) {
  const [username] = useState(currentUser.username)
  const [profilePhoto, setProfilePhoto] = useState(currentUser.profilePhoto || '')
  const [bannerPhoto, setBannerPhoto] = useState(currentUser.bannerPhoto || '')
  const [mediaMessage, setMediaMessage] = useState('')
  const [mediaError, setMediaError] = useState('')
  const [savingMedia, setSavingMedia] = useState(false)
  const avatarInputRef = useRef(null)
  const bannerInputRef = useRef(null)
  const featuredSlots = Array.from({ length: 3 }, (_, index) => featuredBadges[index] || null)

  const persistMedia = async (nextPhoto, nextBanner) => {
    setSavingMedia(true)
    setMediaMessage('')
    setMediaError('')
    try {
      const updated = await updateProfile(username, nextPhoto, nextBanner)
      const savedPhoto = updated.profilePhoto ?? nextPhoto
      const savedBanner = updated.bannerPhoto ?? nextBanner
      setProfilePhoto(savedPhoto || '')
      setBannerPhoto(savedBanner || '')
      onUserUpdated({ ...currentUser, ...updated, profilePhoto: savedPhoto || null, bannerPhoto: savedBanner || null })
      setMediaMessage('Profile media updated successfully.')
    } catch (requestError) {
      setMediaError(requestError.response?.data?.message || requestError.response?.data?.error || 'Could not update your profile media.')
    } finally {
      setSavingMedia(false)
    }
  }

  const readImage = async (event, kind) => {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    if (!file.type.startsWith('image/')) {
      setMediaError('Please select an image file.')
      return
    }
    const maxSize = kind === 'avatar' ? 2 * 1024 * 1024 : 5 * 1024 * 1024
    if (file.size > maxSize) {
      setMediaError(`${kind === 'avatar' ? 'Profile photo' : 'Banner image'} must be smaller than ${kind === 'avatar' ? '2' : '5'} MB.`)
      return
    }
    try {
      setMediaMessage('Preparing image...')
      const dataUrl = await compressImageFile(file, kind === 'avatar'
        ? { maxWidth: 512, maxHeight: 512, quality: 0.82 }
        : { maxWidth: 1600, maxHeight: 480, quality: 0.8 })
      const nextPhoto = kind === 'avatar' ? dataUrl : profilePhoto
      const nextBanner = kind === 'banner' ? dataUrl : bannerPhoto
      if (kind === 'avatar') setProfilePhoto(dataUrl)
      else setBannerPhoto(dataUrl)
      await persistMedia(nextPhoto, nextBanner)
    } catch (error) {
      setMediaError(error.message || 'Could not read that image.')
      setMediaMessage('')
    }
  }

  const saveMedia = async () => {
    await persistMedia(profilePhoto, bannerPhoto)
  }

  const removeMedia = async (kind) => {
    const nextPhoto = kind === 'avatar' ? '' : profilePhoto
    const nextBanner = kind === 'banner' ? '' : bannerPhoto
    if (kind === 'avatar') setProfilePhoto('')
    else setBannerPhoto('')
    await persistMedia(nextPhoto, nextBanner)
  }

  const acceptedSubmissions = submissions.filter((submission) => submission.status === 'ACCEPTED')
  const solvedByDifficulty = ['EASY', 'MEDIUM', 'HARD'].reduce((counts, difficulty) => {
    counts[difficulty] = acceptedSubmissions.filter((submission) => problems.find((problem) => problem.id === submission.problemId)?.difficulty === difficulty).length
    return counts
  }, {})
  const rankIndex = leaderboardData.findIndex((entry) => entry.username === currentUser.username)
  const activityMap = new Map(activity.map((item) => [toLocalDateKey(item.activityDate), item]))
  const days = Array.from({ length: 35 }, (_, index) => {
    const date = new Date()
    date.setDate(date.getDate() - (34 - index))
    return toLocalDateKey(date)
  })

  return <div className="feature-view profile-view">
    <section className="profile-hero panel">
      <button className="profile-cover profile-cover-trigger" type="button" onClick={() => bannerInputRef.current?.click()} style={bannerPhoto ? { backgroundImage: `linear-gradient(90deg, rgba(12, 8, 25, .5), rgba(12, 8, 25, .1)), url(${bannerPhoto})` } : undefined}>
        <span>{bannerPhoto ? 'CHANGE CODE2CAREER PROFILE BANNER' : 'ADD CODE2CAREER PROFILE BANNER'}</span>
      </button>
      <input className="profile-media-input" ref={bannerInputRef} type="file" accept="image/*" onChange={(event) => readImage(event, 'banner')} />
      <div className="profile-hero-body">
        <div className="profile-avatar-editor">
          <button className="profile-avatar-trigger" type="button" onClick={() => avatarInputRef.current?.click()} aria-label="Change profile photo">
            <UserAvatar user={{ ...currentUser, profilePhoto }} className="avatar avatar-violet avatar-profile" />
          </button>
          <input ref={avatarInputRef} type="file" accept="image/*" onChange={(event) => readImage(event, 'avatar')} />
          <div className="profile-media-actions"><button type="button" className="photo-upload" onClick={() => avatarInputRef.current?.click()} disabled={savingMedia}>{profilePhoto ? 'Change photo' : 'Add photo'}</button>{profilePhoto ? <button type="button" className="remove-photo" onClick={() => removeMedia('avatar')} disabled={savingMedia}>Remove photo</button> : null}</div>
        </div>
        <div className="profile-identity"><p className="eyebrow">YOUR DEVELOPER CARD</p><h1>{currentUser.username}</h1><p>Level {currentUser.level || 1} · {currentUser.currentStreak || 0} day streak · Building toward the next milestone</p></div>
        <div className="profile-level"><span>LEVEL</span><strong>{currentUser.level || 1}</strong><small>{(currentUser.xp || 0).toLocaleString()} XP</small></div>
      </div>
      <div className="profile-banner-actions"><button type="button" className="photo-upload" onClick={() => bannerInputRef.current?.click()}>Change banner</button>{bannerPhoto && <button type="button" className="remove-photo" onClick={() => removeMedia('banner')}>Remove banner</button>}<button type="button" className="primary-button" onClick={saveMedia} disabled={savingMedia}>{savingMedia ? 'Saving...' : 'Save media'}</button></div>
      {mediaMessage && <p className="settings-message profile-media-message">{mediaMessage}</p>}
      {mediaError && <p className="auth-error profile-media-message">{mediaError}</p>}
    </section>
    <div className="profile-summary-grid">
      <div className="panel profile-summary-card"><span>GLOBAL RANK</span><strong>{rankIndex >= 0 ? `#${rankIndex + 1}` : '—'}</strong><small>{rankIndex >= 0 ? 'Top 10 leaderboard' : 'Solve a quest to rank'}</small></div>
      <div className="panel profile-summary-card"><span>QUESTS SOLVED</span><strong>{acceptedSubmissions.length}</strong><small>Accepted solutions</small></div>
      <div className="panel profile-summary-card"><span>STREAK</span><strong>{currentUser.currentStreak || 0} days</strong><small>Keep the momentum</small></div>
      <div className="panel profile-summary-card"><span>BADGES</span><strong>{badges.length}</strong><small>Achievements earned</small></div>
    </div>
    <div className="profile-grid">
      <section className="panel solved-panel"><div className="panel-heading"><div><p className="eyebrow">PROBLEM SOLVING</p><h3>Solved by difficulty</h3></div><span className="profile-total">{acceptedSubmissions.length} total</span></div><div className="difficulty-summary">{[['EASY', 'easy'], ['MEDIUM', 'medium'], ['HARD', 'hard']].map(([difficulty, tone]) => <div className="difficulty-summary-row" key={difficulty}><span className={`difficulty ${tone}`}>{difficulty}</span><strong>{solvedByDifficulty[difficulty] || 0}</strong><div className="progress-track"><i style={{ width: `${acceptedSubmissions.length ? Math.min(100, (solvedByDifficulty[difficulty] / acceptedSubmissions.length) * 100) : 0}%` }} /></div></div>)}</div></section>
      <section className="panel featured-profile-panel"><div className="panel-heading"><div><p className="eyebrow">SIGNATURE ACHIEVEMENTS</p><h3>Featured badges</h3></div><span className="profile-total">Up to 3</span></div><div className="profile-badges">{featuredSlots.map((badge, index) => badge ? <button className="profile-badge" type="button" key={badge.code} onClick={() => onBadgeClick(badge)} title={`${badge.name}: ${badge.description}`} aria-label={`View ${badge.name} badge details`}>{badge.code === 'FIRST_CODE' ? <img src="/first-code-badge.png" alt="" /> : <span aria-hidden="true">✦</span>}</button> : <div className="profile-badge profile-badge-empty" key={`empty-slot-${index}`} aria-label="Empty featured badge slot"><span aria-hidden="true">+</span></div>)}{!featuredBadges.length && <p className="muted profile-badge-hint">Pin badges from Achievements to showcase your progress.</p>}</div></section>
    </div>
    <section className="panel activity-panel profile-activity-panel"><div className="panel-heading"><div><p className="eyebrow">RECENT ACTIVITY</p><h3>Your coding rhythm</h3></div><span className="profile-total">Last 35 days</span></div><div className="heatmap">{days.map((day) => { const item = activityMap.get(day); const intensity = item ? Math.min(3, Math.max(1, item.problemsSolved)) : 0; return <div className={`heat-cell heat-${intensity}`} title={`${day}: ${item?.problemsSolved || 0} solved`} key={day} /> })}</div></section>
  </div>
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

function SettingsView({ currentUser, preferences, onPreferencesChange, onUserUpdated }) {
  const [username, setUsername] = useState(currentUser.username)
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
      const updated = await updateProfile(username, currentUser.profilePhoto, currentUser.bannerPhoto)
      onUserUpdated({ ...currentUser, ...updated })
      setMessage('Profile updated successfully.')
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Could not update your profile.')
    } finally {
      setSaving(false)
    }
  }

  const savePassword = async (event) => {
    event.preventDefault()
    setSaving(true); setMessage(''); setError('')
    try { await changePassword(passwords.currentPassword, passwords.newPassword); setPasswords({ currentPassword: '', newPassword: '' }); setMessage('Password changed successfully.') } catch (requestError) { setError(requestError.response?.data?.message || 'Could not change your password.') } finally { setSaving(false) }
  }

  return <div className="feature-view"><div className="feature-heading"><div><p className="eyebrow">APP PREFERENCES</p><h1>Make the arena <span>yours.</span></h1><p className="muted">Choose how Code2Career looks and feels on this device. Preferences are saved locally.</p></div></div><section className="panel settings-card preferences-card"><p className="eyebrow">APPEARANCE & LANGUAGE</p><h3>Personalize your workspace</h3><div className="preference-grid"><label>Theme<select value={preferences.theme} onChange={(event) => onPreferencesChange({ theme: event.target.value })}><option value="dark">Dark (default)</option><option value="light">Light</option></select></label><label>Font size<select value={preferences.fontSize} onChange={(event) => onPreferencesChange({ fontSize: event.target.value })}><option value="small">Small</option><option value="medium">Medium (default)</option><option value="large">Large</option></select></label><label>UI language<select value={preferences.language} onChange={(event) => onPreferencesChange({ language: event.target.value })}><option value="en">English</option><option value="bn">বাংলা (Bangla)</option></select></label></div><p className="preference-note">Language selection is saved now; translation coverage is being rolled out gradually.</p></section><div className="settings-section-heading"><p className="eyebrow">ACCOUNT & SECURITY</p><h2>Manage your account</h2><p className="muted">Update your username here. Profile photo and banner controls live on your public profile.</p></div><div className="settings-grid"><section className="panel settings-card"><p className="eyebrow">PUBLIC IDENTITY</p><h3>Profile details</h3><form onSubmit={saveProfile}><label>Username<input value={username} onChange={(event) => setUsername(event.target.value)} minLength="3" maxLength="50" required /></label><label>Email address<input value={currentUser.email} disabled /></label><button className="primary-button" disabled={saving}>{saving ? 'Saving...' : 'Save profile'}</button></form></section><section className="panel settings-card"><p className="eyebrow">ACCOUNT SECURITY</p><h3>Change password</h3><form onSubmit={savePassword}><label>Current password<input type="password" value={passwords.currentPassword} onChange={(event) => setPasswords({ ...passwords, currentPassword: event.target.value })} required /></label><label>New password<input type="password" value={passwords.newPassword} onChange={(event) => setPasswords({ ...passwords, newPassword: event.target.value })} minLength="8" required /></label><button className="primary-button" disabled={saving}>{saving ? 'Updating...' : 'Update password'}</button></form></section></div>{message && <p className="settings-message">{message}</p>}{error && <p className="auth-error">{error}</p>}</div>
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
  const [activityLoading, setActivityLoading] = useState(true)
  const [activityError, setActivityError] = useState('')
  const [submissions, setSubmissions] = useState([])
  const [roadmaps, setRoadmaps] = useState([])
  const [roadmapsLoading, setRoadmapsLoading] = useState(true)
  const [roadmapsError, setRoadmapsError] = useState('')
  const [authMode, setAuthMode] = useState('login')
  const [user, setUser] = useState({ username: '', email: '', password: '' })
  const [showRegister, setShowRegister] = useState(false)
  const [profileMenuOpen, setProfileMenuOpen] = useState(false)
  const [badgeToReveal, setBadgeToReveal] = useState(null)
  const [preferences, setPreferences] = useState(() => {
    try {
      return { ...preferenceDefaults, ...JSON.parse(getItem('code2career_preferences') || '{}') }
    } catch {
      return preferenceDefaults
    }
  })
  const hasFirstBadge = badges.some((badge) => badge.code === 'FIRST_CODE')
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

  useEffect(() => {
    document.documentElement.dataset.theme = preferences.theme
    document.documentElement.dataset.fontSize = preferences.fontSize
    setItem('code2career_preferences', JSON.stringify(preferences))
  }, [preferences])
  const [message, setMessage] = useState('')

  const refreshActivity = useCallback(async () => {
    const to = new Date()
    const from = new Date()
    from.setDate(to.getDate() - 34)
    setActivityLoading(true)
    setActivityError('')
    try {
      const activityData = await getActivity(toLocalDateKey(from), toLocalDateKey(to))
      setActivity(activityData)
    } catch {
      setActivityError('Could not load activity right now. Please try again.')
    } finally {
      setActivityLoading(false)
    }
  }, [])

  useEffect(() => {
    const token = getItem(appConfig.tokenStorageKey)
    if (!token) return
    getCurrentUser().then((authenticatedUser) => {
      setSelectedProblem(null)
      setActiveNav('Dashboard')
      setSessionUser(authenticatedUser)
    }).catch(() => logout())
  }, [])

  useEffect(() => {
    if (!sessionUser) return
    getLeaderboard().then((entries) => setLeaderboardData(entries.map((entry) =>
      entry.username === sessionUser.username
        ? { ...entry, profilePhoto: sessionUser.profilePhoto }
        : entry
    ))).catch(() => setLeaderboardData([]))
    getBadges().then(setBadges).catch(() => setBadges([]))
    getSubmissions(sessionUser.id).then(setSubmissions).catch(() => setSubmissions([]))
    const to = new Date()
    const from = new Date()
    from.setDate(to.getDate() - 34)
    getActivity(toLocalDateKey(from), toLocalDateKey(to))
      .then((activityData) => {
        setActivity(activityData)
        setActivityError('')
      })
      .catch(() => setActivityError('Could not load activity right now. Please try again.'))
      .finally(() => setActivityLoading(false))
  }, [refreshActivity, sessionUser])

  useEffect(() => {
    if (!sessionUser) return
    getRoadmaps()
      .then((roadmapData) => {
        setRoadmaps(roadmapData)
        setRoadmapsError('')
      })
      .catch((requestError) => {
        setRoadmaps([])
        setRoadmapsError(requestError.response?.data?.message || 'Could not load roadmaps right now.')
      })
      .finally(() => setRoadmapsLoading(false))
  }, [sessionUser, activeNav])

  useEffect(() => {
    if (!sessionUser || !['Challenges', 'PvP', 'Admin', 'Profile'].includes(activeNav)) return
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
    setActivityError('')
    setSubmissions([])
    setRoadmaps([])
    setRoadmapsLoading(false)
    setRoadmapsError('')
  }

  const navigateTo = (destination) => {
    setSelectedProblem(null)
    setProfileMenuOpen(false)
    if (destination === 'Challenges' || destination === 'PvP') setProblemsLoading(true)
    setActiveNav(destination)
    if (destination === 'Profile' || destination === 'Activity') {
      refreshActivity()
    }
    if (destination === 'Leaderboard' || destination === 'Dashboard') {
      getLeaderboard().then(setLeaderboardData).catch(() => setLeaderboardData([]))
    }
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

  const handleSubmissionUpdated = async (updated) => {
    setSubmissions((current) => current.some((submission) => submission.id === updated.id)
      ? current.map((submission) => submission.id === updated.id ? updated : submission)
      : [updated, ...current])

    if (updated.status !== 'ACCEPTED') return

    try {
      const [refreshedUser, refreshedBadges, refreshedSubmissions] = await Promise.all([
        getCurrentUser(),
        getBadges(),
        getSubmissions(sessionUser.id),
      ])
      const firstBadgeWasMissing = !badges.some((badge) => badge.code === 'FIRST_CODE')
      setSessionUser(refreshedUser)
      setBadges(refreshedBadges)
      setSubmissions(refreshedSubmissions)
      await refreshActivity()
      if (firstBadgeWasMissing && refreshedBadges.some((badge) => badge.code === 'FIRST_CODE')) {
        setBadgeToReveal(refreshedBadges.find((badge) => badge.code === 'FIRST_CODE'))
      }
    } catch {
      setActivityError('Your submission was accepted, but some progress panels are still syncing.')
    }
  }

  const players = leaderboardData.slice(0, 3).map((player, index) => ({
      rank: String(index + 1).padStart(2, '0'),
      username: player.username,
      name: player.username,
      handle: `Level ${player.level}`,
      xp: (player.totalXp || 0).toLocaleString(),
      profilePhoto: player.profilePhoto,
      tone: ['violet', 'blue', 'pink'][index],
    }))
  const acceptedSubmissions = submissions.filter((submission) => submission.status === 'ACCEPTED')
  const featuredBadges = badges.filter((badge) => featuredBadgeCodes.includes(badge.code)).slice(0, 3)
  const today = toLocalDateKey()
  const solvedToday = submissions.some((submission) => submission.status === 'ACCEPTED' && toLocalDateKey(submission.submittedAt) === today)
  const completionRate = submissions.length ? Math.round((acceptedSubmissions.length / submissions.length) * 100) : 0
  const dashboardQuests = [
    { title: 'Complete a daily challenge', meta: solvedToday ? 'Complete for today' : 'Not completed today', reward: '+25 XP', progress: solvedToday ? 100 : 0, icon: '⚔' },
    { title: 'Keep your streak alive', meta: `${sessionUser.currentStreak || 0} day streak`, reward: '+15 XP', progress: Math.min(100, ((sessionUser.currentStreak || 0) / 7) * 100), icon: '🔥' },
    { title: 'Build your XP reserve', meta: `${sessionUser.xp || 0} XP earned`, reward: '+40 XP', progress: Math.min(100, ((sessionUser.xp || 0) / 100) * 100), icon: '◈' },
  ]
  const labels = shellLabels[preferences.language] || shellLabels.en

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

        <div className="section-label">{labels.main}</div>
        <nav className="nav-list">
          {[
            ['Dashboard', '⌂'],
            ['Challenges', '◈'],
            ['PvP', '⚔'],
            ['Roadmaps', '⌁'],
            ['Leaderboard', '♛'],
          ].map(([label, icon]) => (
            <button
              className={`nav-item ${activeNav === label ? 'active' : ''}`}
              key={label}
              onClick={() => {
                navigateTo(label)
              }}
            >
              <Icon>{icon}</Icon>
              {labels[label]}
              {label === 'Challenges' && <span className="nav-badge">{problems.length || '—'}</span>}
            </button>
          ))}
        </nav>

        <div className="section-label">{labels.progress}</div>
        <nav className="nav-list">
          <button className={`nav-item ${activeNav === 'Profile' ? 'active' : ''}`} onClick={() => navigateTo('Profile')}><Icon>◎</Icon>{labels.Profile}</button>
          <button className={`nav-item ${activeNav === 'Activity' ? 'active' : ''}`} onClick={() => navigateTo('Activity')}><Icon>◷</Icon>{labels.Activity}</button>
          <button className={`nav-item ${activeNav === 'Achievements' ? 'active' : ''}`} onClick={() => navigateTo('Achievements')}><Icon>✦</Icon>{labels.Achievements}</button>
          <button className={`nav-item ${activeNav === 'Settings' ? 'active' : ''}`} onClick={() => navigateTo('Settings')}><Icon>⚙</Icon>{labels.Settings}</button>
          {sessionUser.role === 'ADMIN' && <button className={`nav-item ${activeNav === 'Admin' ? 'active' : ''}`} onClick={() => navigateTo('Admin')}><Icon>◆</Icon>{labels.Admin}</button>}
        </nav>

        <div className="sidebar-bottom">
          <div className="upgrade-card">
            <div className="upgrade-orb">✦</div>
            <strong>Unlock your potential</strong>
            <p>Master every path with Pro quests.</p>
            <button className="text-button" onClick={() => navigateTo('Roadmaps')}>Explore paths <span>→</span></button>
          </div>
          <div className="user-mini">
            <UserAvatar user={sessionUser} />
            <div><strong>{sessionUser.username}</strong><small>Level {sessionUser.level || 1} · {sessionUser.coins ?? 100} coins</small></div>
            <button className="more-button" aria-label="Open profile menu" onClick={() => setProfileMenuOpen((open) => !open)}>•••</button>
            {profileMenuOpen && <div className="profile-menu"><button onClick={() => navigateTo('Profile')}>{labels.Profile}</button><button onClick={handleLogout}>{labels.logout}</button></div>}
          </div>
        </div>
      </aside>

      <main className="main-content">
        <header className="topbar">
          <img className="mobile-brand" src="/c2c-logo.png" alt="C2C" />
          <div className="breadcrumb"><span>{labels.workspace}</span><b>/</b>{labels[activeNav] || activeNav}</div>
          <div className="top-actions">
            <button className="icon-button" aria-label="Open challenges" onClick={() => navigateTo('Challenges')}>⌕</button>
            <button className="icon-button notification" aria-label="Refresh dashboard" onClick={handleRefresh}>♢<i /></button>
            <button className="profile-chip" onClick={() => navigateTo('Profile')}><UserAvatar user={sessionUser} /><span>{sessionUser.username}</span><b>⌄</b></button>
          </div>
        </header>

        <div className="content-wrap">
          {selectedProblem ? <SolverView key={selectedProblem.id} problem={selectedProblem} onBack={() => navigateTo('Challenges')} onSubmissionUpdated={handleSubmissionUpdated} /> : activeNav === 'Challenges' ? <ChallengesView topics={topics} problems={problems} loading={problemsLoading} onSelectProblem={setSelectedProblem} onTopicChange={(topicId) => { setProblemsLoading(true); getProblems(topicId).then(setProblems).catch(() => setProblems([])).finally(() => setProblemsLoading(false)) }} /> : activeNav === 'PvP' ? <PvpView problems={problems} problemsLoading={problemsLoading} currentUser={sessionUser} onUserUpdate={setSessionUser} /> : activeNav === 'Leaderboard' ? <LeaderboardView entries={leaderboardData} currentUser={sessionUser} /> : activeNav === 'Achievements' ? <AchievementsView badges={badges} featuredCodes={featuredBadgeCodes} onToggleFeatured={toggleFeaturedBadge} currentUser={sessionUser} submissions={submissions} /> : activeNav === 'Profile' ? <ProfileView currentUser={sessionUser} badges={badges} featuredBadges={featuredBadges} submissions={submissions} activity={activity} leaderboardData={leaderboardData} problems={problems} onUserUpdated={setSessionUser} onBadgeClick={setBadgeToReveal} /> : activeNav === 'Activity' ? <ActivityView activity={activity} currentUser={sessionUser} loading={activityLoading} error={activityError} onRetry={refreshActivity} /> : activeNav === 'Roadmaps' ? <RoadmapsView roadmaps={roadmaps} loading={roadmapsLoading} error={roadmapsError} onOpenChallenges={() => navigateTo('Challenges')} /> : activeNav === 'Settings' ? <SettingsView currentUser={sessionUser} preferences={preferences} onPreferencesChange={(changes) => setPreferences((current) => ({ ...current, ...changes }))} onUserUpdated={setSessionUser} /> : activeNav === 'Admin' && sessionUser.role === 'ADMIN' ? <AdminView topics={topics} problems={problems} onDataChanged={() => { getTopics().then(setTopics); getProblems().then(setProblems) }} /> : <>
          <section className="welcome-row">
            <div>
              <p className="eyebrow">{new Intl.DateTimeFormat('en-US', { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' }).format(new Date()).toUpperCase()}</p>
              <h1>Welcome back, <span>{sessionUser.username}</span> <em>✦</em></h1>
              <p className="muted">Your next level is closer than you think. Keep building.</p>
            </div>
            <button className="primary-button" onClick={() => navigateTo('Challenges')}><span>＋</span> Start a challenge</button>
          </section>

          <section className="hero-card">
            <div className="hero-copy">
              <div className="status-pill"><i /> CURRENT QUEST</div>
              <h2>Forge your <span>legend.</span></h2>
              <p>Complete challenges, sharpen your skills, and rise through the ranks.</p>
              <button className="hero-button" onClick={() => navigateTo('Challenges')}>Continue quest <span>→</span></button>
            </div>
            <div className="hero-art" aria-hidden="true">
              <div className="moon" />
              <div className="ring ring-one" /><div className="ring ring-two" />
              <div className="silhouette"><div className="head" /><div className="body" /><div className="sword" /></div>
              <div className="spark spark-one">✦</div><div className="spark spark-two">✧</div>
            </div>
            <div className="hero-level"><span>LEVEL</span><strong>{sessionUser.level || 1}</strong><small>DEVELOPER</small></div>
          </section>

          {hasFirstBadge && <button className="first-badge-card panel" onClick={() => setBadgeToReveal(badges.find((badge) => badge.code === 'FIRST_CODE'))} aria-label="Open First Code achievement">
            <img src="/first-code-badge.png" alt="First Code achievement badge" />
            <span><small>ACHIEVEMENT UNLOCKED</small><strong>First Code</strong><em>A small step towards bigger things · Click to inspect</em></span>
            <b>↗</b>
          </button>}

          <section className="stat-grid">
            <div className="stat-card"><div className="stat-icon purple">✦</div><div><span>TOTAL XP</span><strong>{(sessionUser.xp || 0).toLocaleString()}</strong><small><b>Live profile</b> current XP</small></div></div>
            <div className="stat-card"><div className="stat-icon orange">🔥</div><div><span>DAY STREAK</span><strong>{sessionUser.currentStreak || 0} days</strong><small><b>Keep going</b> every day</small></div></div>
            <div className="stat-card"><div className="stat-icon blue">♛</div><div><span>GLOBAL RANK</span><strong>{leaderboardData.findIndex((entry) => entry.username === sessionUser.username) >= 0 ? `#${leaderboardData.findIndex((entry) => entry.username === sessionUser.username) + 1}` : '—'}</strong><small><b>Live ranking</b> top 10</small></div></div>
            <div className="stat-card"><div className="stat-icon green">✓</div><div><span>QUESTS CLEARED</span><strong>{acceptedSubmissions.length}</strong><small><b>{completionRate}%</b> completion rate</small></div></div>
          </section>

          <div className="dashboard-grid">
            <section className="panel quests-panel">
              <div className="panel-heading"><div><p className="eyebrow">DAILY OBJECTIVES</p><h3>Active quests</h3></div><button className="link-button" onClick={() => navigateTo('Challenges')}>View all <span>→</span></button></div>
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
              <div className="panel-heading"><div><p className="eyebrow">THE HALL OF FAME</p><h3>Leaderboard</h3></div><button className="link-button" onClick={() => navigateTo('Leaderboard')}>Full ranking <span>→</span></button></div>
              <div className="leader-list">
                {players.length ? players.map((player) => <div className="leader-row" key={player.rank}><b className="rank">{player.rank}</b><UserAvatar user={player} className={`avatar avatar-${player.tone}`} /><div className="leader-name"><strong>{player.name}</strong><small>{player.handle}</small></div><strong className="leader-xp">{player.xp} <small>XP</small></strong></div>) : <div className="empty-state compact-empty">No leaderboard data yet.</div>}
              </div>
              <div className="your-rank"><span>Your current rank</span><strong>{leaderboardData.findIndex((entry) => entry.username === sessionUser.username) >= 0 ? `#${leaderboardData.findIndex((entry) => entry.username === sessionUser.username) + 1}` : 'Not ranked'}</strong><b>{leaderboardData.length ? 'Live' : 'Pending'}</b></div>
            </section>
          </div>

          <section className="panel roadmap-panel">
            <div className="panel-heading"><div><p className="eyebrow">YOUR JOURNEY</p><h3>Continue learning</h3></div><button className="link-button" onClick={() => navigateTo('Roadmaps')}>Browse roadmaps <span>→</span></button></div>
            <div className="roadmap-list">
              {roadmaps.length ? roadmaps.slice(0, 2).map((roadmap, index) => <div className={`roadmap-card ${index === 0 ? 'featured-roadmap' : ''}`} key={roadmap.id}><div className={`roadmap-icon ${index > 0 ? 'blue-bg' : ''}`}>{roadmap.icon}</div><div><span>{roadmap.title.toUpperCase()} · {roadmap.progressPercent >= 100 ? 'COMPLETED' : roadmap.progressPercent > 0 ? 'IN PROGRESS' : 'RECOMMENDED'}</span><h4>{roadmap.title}</h4><p>{roadmap.description}</p>{roadmap.progressPercent >= 100 ? <div className="completed-label">✓ Completed</div> : <div className="roadmap-progress"><i style={{ width: `${roadmap.progressPercent}%` }} /><small>{roadmap.progressPercent}% complete</small></div>}</div><button className="round-arrow" onClick={() => navigateTo('Roadmaps')}>→</button></div>) : <div className="empty-state compact-empty">Your learning paths are loading.</div>}
            </div>
          </section>
          </>}
        </div>
      </main>

      {showRegister && <div className="modal-backdrop" onClick={() => setShowRegister(false)}><div className="register-modal" onClick={(event) => event.stopPropagation()}><button className="modal-close" onClick={() => setShowRegister(false)}>×</button><p className="eyebrow">JOIN THE GUILD</p><h2>Create your developer profile</h2><p className="muted">Start your journey and earn your first XP.</p><form onSubmit={handleRegister}><input name="username" placeholder="Username" onChange={handleChange} required /><input name="email" type="email" placeholder="Email address" onChange={handleChange} required /><input name="password" type="password" placeholder="Password (8+ characters)" onChange={handleChange} required minLength="8" /><button className="primary-button" type="submit">Begin your quest <span>→</span></button></form>{message && <p className="form-message">{message}</p>}</div></div>}
      {badgeToReveal && <div className="badge-reveal-backdrop" onClick={() => setBadgeToReveal(null)}><div className="badge-reveal" onClick={(event) => event.stopPropagation()}><button className="modal-close" onClick={() => setBadgeToReveal(null)}>×</button><div className="badge-reveal-glow" />{badgeToReveal.code === 'FIRST_CODE' ? <img src="/first-code-badge.png" alt={`${badgeToReveal.name} badge`} /> : <div className="badge-reveal-generic">✦</div>}<p className="eyebrow">ACHIEVEMENT UNLOCKED</p><h2>{badgeToReveal.name.toUpperCase()}</h2><p>{badgeToReveal.description}</p><button className="primary-button" onClick={() => setBadgeToReveal(null)}>Continue your quest <span>→</span></button></div></div>}
    </div>
  )
}

export default App
