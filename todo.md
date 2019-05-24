# letsdo.events

- send mail
  - use simple smtp from login@letsdo.events
  - email template


- HTTPS via letsencrypt support
  - do in nginx



- sorting of event list
- event listing filters
- event listing search

- escape html when rendering

- catch errors and never show to client
- nice error/spec messages for dev

- allow editing slug and check for conflicts

- image resizing

- crop image size in browser


- confirm delete event
- confirm delete topic

- allow adding and removing organizers


- use same data for schema and for key selection
- put server in ctx
- allow app to be running at sub-route instead of top level

- drag&drop image upload

- list all your topics
- list users by topic

- CRUD for topic admin

- user settings at /me
- delete account

- notify about new activity (via mail) if subscribed to topic
  - admin subscribes automatically
  - can unsubscribe
- notify about updated to activity (via mail) if subscribed
  - joining or interest subscribe automatically
  - can unsubscribe
- calendar integration

- limit input char length (against evil input)
- spec for API responses
- security headers
- gzip
- html meta tags
- cache css
- imprint and cookie warning
- [cookie settings](https://github.com/ring-clojure/ring/wiki/Cookies)
- check for weak password
- cache pages
- login delay against brute force
- protect form forgery: form nonce
- stats
- [prevent redirect attacks](https://rundis.github.io/blog/2015/buddy_auth_part2.html)
- monitoring/error tracking

- handle display of multiple anonymous organizers

- how to delete unused images?

- manifold vs core.async for transaction

- edit view: image hover

- request joining topic
- anyone can join/invite only
- invite
- cancel invite

- suggest activity/talk/thing
- event interest
- schedule/commit

- Filter by organizer
- confirm() before unjoin

- setup possible dates

- caching headers

- Gravatar
- API keys
- json export
- interact with events/comments/app via email
  - buttons to join, show interest, ...
  - reply to comment
- integrate with Meetup.com
  - sync to meetup.com
  - sync form meetup.com
- i18n
- a11y

- validate image dimension on server-side

- data generation via API+specs
- use API specs for generative testing