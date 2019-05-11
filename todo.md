# letsdo.events

## Tech

- redirect empty /for and /about one up
- sorting of event list
- event listing filters
- event listing search
- redirect unauthorized to login with redirect URI
    letsdo.events/login?goto=heart-of-clojure/about/cljdoc-hacking
- error page
- delete event
- change cookie to JWT
  - then: login email only
- save type with each document
- helper to delete all data of a kind

- topic edit view (use CAS transaction for editing)
  - only by admin
- event edit view
- allow editing slug and check for conflicts
- edit only by organizer
- image resizing
- image preview
- [clear image input](https://www.w3schools.com/howto/howto_html_clear_input.asp)

- allow app to be running at sub-route instead of top level
- catch errors and never show to client
- db location from config
- config defaults
- spec for config
- production router
- production build
- nice error/spec messages for dev
- repair on read: when reading things that are supposed to be unique but are not, throw away invalid ones
  - read user by email: throw away all but the first in time if there is more than one

- topic settings page
- list all your topics
- delete topic
- list users by topic

- make creator topic admin
- CRUD for topic admin
- multiple organizers per event

- user settings at /me
- delete account

- list public topics

- notify about new activity (via mail)
- calendar integration

- format event date nicely and handle missing values
- event times: support invalid dates / try to correct format
- include tachyons locally
- limit input char length (against to evil input)
- use your own colors
- spec for API responses
- security headers
- add introduction text
- gzip
- html meta tags
- cache css
- imprint and cookie warning
- storing images as binary in crux
- [cookie settings](https://github.com/ring-clojure/ring/wiki/Cookies)
- js as progressive enhancement
- check for weak password
- cache pages
- login delay against brute force
- protect form forgery: form nonce
- stats
- [prevent redirect attacks](https://rundis.github.io/blog/2015/buddy_auth_part2.html)
- monitoring/error tracking

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

- login theme depending on goto parameter (styles of topic)

- Gravatar
- API keys
- json export
- interact with events/comments/app via email
- integrate with Meetup.com
  - sync to meetup.com
  - sync form meetup.com
- i18n
- a11y

- data generation via API+specs
- use API specs for generative testing