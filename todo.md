# letsdo.events

- add event interest
- remove event interest
- sort by event interest

- notify about new activity (via mail) if subscribed to topic
  - admin subscribes automatically
  - can unsubscribe
- notify about updated to activity (via mail) if subscribed
  - joining or interest subscribe automatically
  - can unsubscribe
- List all your subscriptions

- how to delete unused images?
 can only delete if no event and no topic references it
 who should track cascading references?
 have a spec for db schema
 have an image type
 find by attribute that uses image type

- user settings at /me
  - delete account
- multiple organizers
  - Request joining organizers
  - Accept organizer
  - Stop organizing
  - Remove an organizer
- CRUD for topic admin

- nice error/spec messages for dev
- catch errors and never show to client

- find clear way to represent DB schema

- limit input char length (against evil input)
- [security headers](https://github.com/ring-clojure/ring-defaults/blob/master/src/ring/middleware/defaults.clj) and [score](https://observatory.mozilla.org/analyze/letsdo.events)
- login delay against brute force
- protect form forgery: form nonce
- [prevent redirect attacks](https://rundis.github.io/blog/2015/buddy_auth_part2.html)
- caching headers

- manifold vs core.async for transaction

- readme with setup instructions

- Refactor links and goto logic in a helper
  - allow app to be running at sub-route instead of top level

- events instead of metrics and logging: one event with all the context per request
  - try honeycomb free?
    https://github.com/conormcd/clj-honeycomb
    https://github.com/getsentry/sentry-clj
    https://github.com/sethtrain/raven-clj
    https://github.com/exoscale/raven

- allow editing slug and check for conflicts
- image resizing
- crop image size in browser
- drag&drop image upload

- Leaving a topic

- calendar integration
- How to prevent mail and sign up spam?
- Pagination for event and topic list
- spec for API responses

- imprint and cookie warning

- stats
- check for weak password
- cache pages
- handle display of multiple anonymous organizers
- edit view: image hover
- empty topic placeholder
- request joining topic
- favicon
- setup possible dates
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
- data generation via API+specs
- use API specs for generative testing

- robots.txt and meta tag
- cache css rendering
- meta open graph tags
