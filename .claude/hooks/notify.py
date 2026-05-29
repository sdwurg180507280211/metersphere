import sys
import os
import json
import traceback

import tkinter as tk

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
ERROR_LOG = os.path.join(SCRIPT_DIR, 'notify_error.log')

if sys.platform == 'win32':
    try:
        import ctypes
        ctypes.windll.user32.ShowWindow(ctypes.windll.kernel32.GetConsoleWindow(), 0)
    except Exception:
        pass

# cross-platform fonts
if sys.platform == 'win32':
    FONT = 'Microsoft YaHei UI'
    MONO = 'Consolas'
elif sys.platform == 'darwin':
    FONT = 'Helvetica Neue'
    MONO = 'Menlo'
else:
    FONT = 'Noto Sans'
    MONO = 'DejaVu Sans Mono'

C = {
    'bg':      '#f8f9fa',
    'card':    '#ffffff',
    'text':    '#212529',
    'accent':  '#4361ee',
    'accent2': '#3a56d4',
    'danger':  '#e63946',
    'danger2': '#c5303c',
    'border':  '#dee2e6',
    'bar_bg':  '#f1f3f5',
    'btn_allow':       '#1a73e8',
    'btn_allow_hover': '#1557b0',
    'btn_allow_text':  '#ffffff',
    'btn_remember':       '#34a853',
    'btn_remember_hover': '#2d8f47',
    'btn_remember_text':  '#ffffff',
    'btn_deny':       '#f1f3f5',
    'btn_deny_hover': '#e2e5e9',
    'btn_deny_text':  '#5f6368',
}

def _center(root, w, h):
    sw = root.winfo_screenwidth()
    sh = root.winfo_screenheight()
    root.geometry(f'{w}x{h}+{(sw-w)//2}+{(sh-h)//2}')


def _focus(root, default_btn):
    root.lift()
    root.attributes('-topmost', True)
    root.after_idle(root.attributes, '-topmost', False)
    root.focus_force()
    default_btn.focus_set()


def _make_btn(parent, text, command, bg_color, fg_color, hover_color, font, **kw):
    btn = tk.Label(parent, text=text, bg=bg_color, fg=fg_color, font=font, cursor='hand2', **kw)
    def on_enter(e):
        btn.configure(bg=hover_color)
    def on_leave(e):
        btn.configure(bg=bg_color)
    def on_click(e):
        command()
    btn.bind('<Enter>', on_enter)
    btn.bind('<Leave>', on_leave)
    btn.bind('<Button-1>', on_click)
    return btn


def _extract_detail(tool_input):
    lines = []
    for k, v in tool_input.items():
        if k == 'description':
            continue
        if isinstance(v, str):
            lines.append(f'{k}: {v}')
        elif isinstance(v, (int, float, bool)):
            lines.append(f'{k}: {v}')
        elif isinstance(v, dict):
            lines.append(f'{k}: {json.dumps(v, ensure_ascii=False)}')
        elif isinstance(v, list):
            lines.append(f'{k}: {json.dumps(v, ensure_ascii=False)}')
    return '\n'.join(lines)




def show_permission_dialog(data):
    tool_name = data.get('tool_name', 'Unknown')
    tool_input = data.get('tool_input', {})
    suggestions = data.get('permission_suggestions', [])

    decision = {'behavior': 'deny'}

    root = tk.Tk()
    root.title('Claude Code · 权限申请')
    root.resizable(False, False)
    root.configure(bg=C['bg'])

    n = len(suggestions)
    win_h = 260 + min(n, 4) * 38
    _center(root, 480, win_h)

    header = tk.Frame(root, bg=C['bg'])
    header.pack(fill='x', padx=20, pady=(18, 10))
    tk.Label(header, text='Claude 需要授权',
             font=(FONT, 12, 'bold'), fg=C['text'], bg=C['bg']).pack(anchor='w')

    card = tk.Frame(root, bg=C['card'], padx=14, pady=10,
                    highlightbackground=C['border'], highlightthickness=1)
    card.pack(fill='x', padx=16, pady=(0, 10))

    tk.Label(card, text=tool_name,
             font=(MONO, 10, 'bold'), fg=C['accent'], bg=C['card']).pack(anchor='w')

    detail = _extract_detail(tool_input)
    if detail:
        detail_frame = tk.Frame(card, bg=C['card'])
        detail_frame.pack(fill='x', pady=(4, 0))

        detail_text = tk.Text(detail_frame, font=(MONO, 13), fg=C['text'], bg=C['card'],
                              wrap='word', height=4, bd=0, padx=0, pady=0,
                              cursor='arrow', relief='flat')
        detail_text.insert('1.0', detail)
        detail_text.configure(state='disabled')
        detail_text.pack(side='left', fill='both', expand=True)

        scrollbar = tk.Scrollbar(detail_frame, command=detail_text.yview, bd=0,
                                 elementborderwidth=0, highlightthickness=0)
        scrollbar.pack(side='right', fill='y')
        detail_text.configure(yscrollcommand=scrollbar.set)

    def on_allow_once():
        decision['behavior'] = 'allow'
        root.destroy()

    def on_allow_rule(sugg):
        decision['behavior'] = 'allow'
        decision['updatedPermissions'] = [sugg]
        root.destroy()

    def on_deny():
        root.destroy()

    bar = tk.Frame(root, bg=C['bar_bg'], padx=16, pady=12)
    bar.pack(fill='x', side='bottom')

    tk.Frame(root, bg=C['border'], height=1).pack(fill='x', side='bottom')

    btn_deny = _make_btn(bar, '  拒绝  ', on_deny,
                         C['btn_deny'], C['btn_deny_text'], C['btn_deny_hover'],
                         (FONT, 10), padx=12, pady=6)
    btn_deny.pack(side='right', padx=(8, 0))

    if not suggestions:
        suggestions = [{
            'type': 'addRules',
            'rules': [{'toolName': tool_name}],
            'behavior': 'allow',
            'destination': 'localSettings',
        }]
    btn = _make_btn(bar, '  同意并记住  ', lambda s=suggestions[0]: on_allow_rule(s),
                    C['btn_remember'], C['btn_remember_text'], C['btn_remember_hover'],
                    (FONT, 10), padx=10, pady=6)
    btn.pack(side='right', padx=(0, 6))

    btn_allow = _make_btn(bar, '  同意  ', on_allow_once,
                          C['btn_allow'], C['btn_allow_text'], C['btn_allow_hover'],
                          (FONT, 10, 'bold'), padx=16, pady=6)
    btn_allow.pack(side='right', padx=(0, 8))

    _focus(root, btn_allow)
    root.bind('<Escape>', lambda e: on_deny())
    root.bind('<Return>', lambda e: on_allow_once())

    root.mainloop()
    return decision


def show_idle_dialog(data):
    root = tk.Tk()
    root.title('Claude Code')
    root.resizable(False, False)
    root.configure(bg=C['bg'])
    _center(root, 320, 120)

    main = tk.Frame(root, bg=C['bg'], padx=24, pady=20)
    main.pack(fill='both', expand=True)

    tk.Label(main, text='Claude 已完成，等待输入',
             font=(FONT, 11), fg=C['text'], bg=C['bg']).pack()

    def close():
        root.destroy()

    btn = _make_btn(main, '  知道了  ', close,
                    C['btn_allow'], C['btn_allow_text'], C['btn_allow_hover'],
                    (FONT, 9, 'bold'), padx=20, pady=5)
    btn.pack(pady=(14, 0))

    _focus(root, btn)
    root.bind('<Escape>', lambda e: close())
    root.bind('<Return>', lambda e: close())

    root.mainloop()


# ── entry ──────────────────────────────────────────────────────────────
def main():
    try:
        raw = sys.stdin.buffer.read()
        if not raw:
            return
        data = json.loads(raw.decode())
    except Exception:
        with open(ERROR_LOG, 'a', encoding='utf-8') as f:
            f.write(f'[stdin-error] {traceback.format_exc()}\n')
        return

    event = data.get('hook_event_name', '')

    try:
        if event == 'PermissionRequest':
            decision = show_permission_dialog(data)
            output = {
                'hookSpecificOutput': {
                    'hookEventName': 'PermissionRequest',
                    'decision': decision,
                }
            }
            print(json.dumps(output, ensure_ascii=False))
        elif event == 'Notification':
            show_idle_dialog(data)
    except Exception:
        with open(ERROR_LOG, 'a', encoding='utf-8') as f:
            f.write(f'[gui-error] {traceback.format_exc()}\n')


if __name__ == '__main__':
    main()
