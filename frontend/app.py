import html
import os
from datetime import date

import requests
import streamlit as st


API_BASE_URL = os.getenv("EMAIL_API_URL", "http://localhost:8082").rstrip("/")
REQUEST_TIMEOUT = 120

st.set_page_config(
    page_title="Draftly · Email workspace",
    page_icon="✉️",
    layout="wide",
    initial_sidebar_state="expanded",
)

st.markdown(
    """
    <style>
    @import url('https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&family=Manrope:wght@400;500;600;700;800&display=swap');
    :root {
      color-scheme:dark;
      --background:#0F172A;
      --sidebar:#1E293B;
      --card:#334155;
      --primary:#3B82F6;
      --heading:#F8FAFC;
      --body:#E2E8F0;
      --secondary:#CBD5E1;
      --line:#475569;
    }
    html, body, [class*="css"] { font-family:'DM Sans',sans-serif; }
    .stApp, [data-testid="stAppViewContainer"] { background:var(--background); color:var(--body); }
    [data-testid="stHeader"] { background:rgba(15,23,42,.96); }
    [data-testid="stSidebar"] { background:var(--sidebar); border-right:1px solid var(--line); }
    [data-testid="stSidebar"] > div:first-child { padding-top:1.4rem; }
    .block-container { max-width:1240px; padding-top:2.2rem; padding-bottom:4rem; }
    h1,h2,h3,h4,h5,h6 { font-family:'Manrope',sans-serif; color:var(--heading); letter-spacing:-.035em; }
    h1 { font-size:2.3rem !important; font-weight:800 !important; }
    h2 { font-size:1.35rem !important; font-weight:750 !important; }
    p, label, .stCaption, [data-testid="stMarkdownContainer"], [data-testid="stMarkdownContainer"] p,
    [data-testid="stWidgetLabel"] p { color:var(--body); }
    [data-testid="stCaptionContainer"], [data-testid="stCaptionContainer"] p { color:var(--secondary) !important; }
    .brand { font-family:'Manrope',sans-serif; font-weight:800; font-size:1.4rem; color:var(--heading); letter-spacing:-.06em; }
    .brand span { color:#60A5FA; }
    .eyebrow { color:#93C5FD; font-size:.76rem; text-transform:uppercase; font-weight:700; letter-spacing:.13em; margin-bottom:.45rem; }
    .hero { background:var(--card); border:1px solid var(--line); border-radius:22px; padding:1.7rem 2rem; margin-bottom:1.4rem; }
    .hero p { color:var(--body); margin:0; font-size:.98rem; }
    .card { background:var(--card); border:1px solid var(--line); border-radius:18px; padding:1.35rem 1.45rem; margin:.45rem 0 1rem; box-shadow:0 5px 22px rgba(0,0,0,.18); color:var(--body); }
    .reply-card { background:var(--card); border-color:var(--line); }
    .small-label { color:var(--secondary); font-size:.75rem; font-weight:700; text-transform:uppercase; letter-spacing:.09em; }
    .stTextInput input, .stTextArea textarea, [data-baseweb="select"] > div {
      border-radius:11px !important; border-color:var(--line) !important; background:var(--sidebar) !important; color:var(--heading) !important;
    }
    .stTextInput input::placeholder, .stTextArea textarea::placeholder { color:var(--secondary) !important; opacity:1; }
    .stButton > button, .stFormSubmitButton > button {
      border-radius:11px; min-height:2.8rem; font-weight:700; border:1px solid var(--primary); background:var(--primary); color:#FFFFFF;
    }
    .stButton > button:hover, .stFormSubmitButton > button:hover {
      background:#2563EB; border-color:#2563EB; color:#FFFFFF;
    }
    .stButton > button:focus, .stFormSubmitButton > button:focus { color:#FFFFFF; outline:2px solid #BFDBFE; outline-offset:2px; }
    [data-testid="stMetric"] { background:var(--card); border:1px solid var(--line); border-radius:15px; padding:1rem 1.15rem; }
    [data-testid="stMetricLabel"], [data-testid="stMetricLabel"] p { color:var(--secondary) !important; }
    [data-testid="stMetricValue"] { color:var(--heading) !important; }
    [data-testid="stExpander"] { background:var(--card); border:1px solid var(--line); border-radius:12px; }
    [data-testid="stAlert"] { color:var(--heading); }
    a { color:#93C5FD; }
    hr { border-color:var(--line); }
    </style>
    """,
    unsafe_allow_html=True,
)


def api_request(method, path, **kwargs):
    try:
        response = requests.request(
            method,
            f"{API_BASE_URL}{path}",
            timeout=REQUEST_TIMEOUT,
            **kwargs,
        )
    except requests.RequestException as exc:
        raise RuntimeError(
            f"Could not reach the email API at {API_BASE_URL}. "
            "Make sure the Spring Boot app is running."
        ) from exc

    if not response.ok:
        try:
            payload = response.json()
            detail = payload.get("message") or payload.get("detail") or payload
        except ValueError:
            detail = response.text or response.reason
        raise RuntimeError(f"API returned HTTP {response.status_code}: {detail}")

    if not response.content:
        return None
    return response.json()


def show_task(task):
    title = html.escape(str(task.get("title") or "Untitled task"))
    owner = html.escape(str(task.get("owner") or "Unassigned"))
    due_date = html.escape(str(task.get("dueDate") or "No due date"))
    priority = html.escape(str(task.get("priority") or "NORMAL"))
    st.markdown(
        f"""
        <div class="card">
          <div style="display:flex;justify-content:space-between;gap:1rem;align-items:center">
            <div><strong>{title}</strong><br>
              <span style="color:#CBD5E1;font-size:.86rem">{owner} · {due_date}</span>
            </div>
            <span style="background:#1E293B;color:#F8FAFC;padding:.35rem .65rem;border:1px solid #64748B;border-radius:99px;
              font-size:.72rem;font-weight:700">{priority}</span>
          </div>
        </div>
        """,
        unsafe_allow_html=True,
    )


with st.sidebar:
    st.markdown('<div class="brand">draft<span>ly</span></div>', unsafe_allow_html=True)
    st.caption("YOUR AI EMAIL WORKSPACE")
    st.divider()

    if st.button("✉️  Compose an email", use_container_width=True, type="primary"):
        st.session_state["active_view"] = "Compose"
    if st.button("◷  Email history", use_container_width=True):
        st.session_state["active_view"] = "History"
    if st.button("✓  Extracted tasks", use_container_width=True):
        st.session_state["active_view"] = "Tasks"

    st.divider()
    st.markdown('<span class="small-label">CONNECTION</span>', unsafe_allow_html=True)
    try:
        api_request("GET", "/v3/api-docs")
        st.success("Email assistant is online", icon="✅")
    except RuntimeError:
        st.error("Email assistant is offline", icon="⚠️")
        st.caption(f"API: {API_BASE_URL}")
    st.caption(f"Today · {date.today().strftime('%B %d, %Y')}")


active_view = st.session_state.get("active_view", "Compose")

if active_view == "Compose":
    st.markdown(
        """
        <div class="hero">
          <div class="eyebrow">YOUR INBOX, WITH A LITTLE HELP</div>
          <h1 style="margin:0 0 .35rem">Turn emails into action.</h1>
          <p>Get the key intent, follow-up tasks, meeting ideas, and a polished reply in one go.</p>
        </div>
        """,
        unsafe_allow_html=True,
    )

    compose_col, guide_col = st.columns([1.55, 0.85], gap="large")
    with compose_col:
        st.markdown("## Compose")
        with st.form("email_form", clear_on_submit=False):
            subject = st.text_input("Subject", placeholder="e.g. Project timeline and next steps")
            email_content = st.text_area(
                "Paste the email you received",
                height=245,
                placeholder=(
                    "Hi Alex,\n\nCould we meet next week to review the launch plan? "
                    "Please send the updated slides by Friday..."
                ),
            )
            submitted = st.form_submit_button("✦  Analyze email & draft reply", use_container_width=True)

        if submitted:
            if not email_content.strip():
                st.warning("Add the email content first and I’ll take it from there.")
            else:
                with st.spinner("Reading the email and drafting a thoughtful reply…"):
                    try:
                        st.session_state["latest_result"] = api_request(
                            "POST",
                            "/api/email/process",
                            json={"subject": subject.strip() or None, "email": email_content.strip()},
                        )
                        st.session_state["latest_subject"] = subject.strip() or "Untitled email"
                        st.session_state["latest_email"] = email_content.strip()
                    except RuntimeError as exc:
                        st.error(str(exc))

    with guide_col:
        st.markdown("## Your workflow")
        st.markdown(
            """
            <div class="card">
              <div class="eyebrow">01 · UNDERSTAND</div>
              <strong>Know what matters</strong><br>
              <span style="color:#CBD5E1">AI identifies the email’s intent and key asks.</span>
            </div>
            <div class="card">
              <div class="eyebrow">02 · ORGANIZE</div>
              <strong>Make the next steps clear</strong><br>
              <span style="color:#CBD5E1">Turn follow-ups into tasks and find meeting options.</span>
            </div>
            <div class="card">
              <div class="eyebrow">03 · RESPOND</div>
              <strong>Start with a polished draft</strong><br>
              <span style="color:#CBD5E1">Review and edit a reply before sending it yourself.</span>
            </div>
            """,
            unsafe_allow_html=True,
        )
        st.caption("Your draft is for review. Nothing is sent automatically.")

    result = st.session_state.get("latest_result")
    if result:
        st.divider()
        st.markdown("## Your email brief")
        metric_cols = st.columns(3)
        metric_cols[0].metric("Intent", result.get("intent") or "—")
        metric_cols[1].metric("Follow-up tasks", len(result.get("tasks") or []))
        metric_cols[2].metric("Meeting options", len(result.get("meetingSuggestions") or []))

        reply_col, details_col = st.columns([1.2, 0.8], gap="large")
        with reply_col:
            st.markdown("### Reply draft")
            st.text_area(
                "Review and edit your reply",
                value=result.get("reply") or "",
                height=250,
                key="reply_editor",
                label_visibility="collapsed",
            )
            st.caption("Edit the draft here, then copy it into your email when you’re ready.")
        with details_col:
            st.markdown("### Follow-up tasks")
            tasks = result.get("tasks") or []
            if tasks:
                for task in tasks:
                    show_task(task)
            else:
                st.info("No follow-up tasks were identified.")

            st.markdown("### Suggested meeting times")
            suggestions = result.get("meetingSuggestions") or []
            if suggestions:
                for suggestion in suggestions:
                    st.markdown(f"- {suggestion}")
            else:
                st.caption("No meeting times suggested for this email.")

elif active_view == "History":
    st.markdown("## Email history")
    st.caption("A look back at the emails you’ve already processed.")
    try:
        page = api_request("GET", "/api/emails", params={"page": 0, "size": 20, "sort": "createdAt,desc"})
        entries = page.get("content", []) if isinstance(page, dict) else []
        if not entries:
            st.info("Your processed emails will show up here.")
        for entry in entries:
            title = entry.get("subject") or "Untitled email"
            with st.expander(f"{title}  ·  {entry.get('intent', 'GENERAL')}"):
                st.caption(f"Processed {entry.get('createdAt', 'date unavailable')}")
                if entry.get("sender"):
                    st.write(f"**From:** {entry['sender']}")
                st.write(f"**Intent:** {entry.get('intent', '—')}")
                if entry.get("replyDraft"):
                    st.text_area(
                        "Reply draft",
                        value=entry["replyDraft"],
                        key=f"history_reply_{entry.get('id', title)}",
                        height=150,
                    )
                for task in entry.get("tasks") or []:
                    show_task(task)
    except RuntimeError as exc:
        st.error(str(exc))

else:
    st.markdown("## Extracted tasks")
    st.caption("Keep the follow-ups from your emails in one place.")
    try:
        tasks = api_request("GET", "/api/tasks")
        if not tasks:
            st.info("Tasks extracted from processed emails will appear here.")
        for task in tasks or []:
            show_task(task)
    except RuntimeError as exc:
        st.error(str(exc))
