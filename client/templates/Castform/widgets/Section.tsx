import { Email, Link, Phone } from '@mui/icons-material';
import { ListItem, Section as SectionType } from '@reactive-resume/schema';
import get from 'lodash/get';
import isArray from 'lodash/isArray';
import isEmpty from 'lodash/isEmpty';

import Markdown from '@/components/shared/Markdown';
import { useAppSelector } from '@/store/hooks';
import { SectionProps } from '@/templates/sectionMap';
import DataDisplay from '@/templates/shared/DataDisplay';
import { formatDateString } from '@/utils/date';
import { parseListItemPath } from '@/utils/template';

import Heading from './Heading';

const Section: React.FC<SectionProps> = ({
  path,
  titlePath = 'title',
  subtitlePath = 'subtitle',
  headlinePath = 'headline',
  keywordsPath = 'keywords',
}) => {
  const section: SectionType = useAppSelector((state) => get(state.resume, path, {}));
  const dateFormat: string = useAppSelector((state) => get(state.resume, 'metadata.date.format'));

  if (!section.visible) return null;

  if (isArray(section.items) && isEmpty(section.items)) return null;

  return (
    <section>
      <Heading>{section.name}</Heading>

      <div
        className="grid items-start gap-4"
        style={{ gridTemplateColumns: `repeat(${section.columns}, minmax(0, 1fr))` }}
      >
        {section.items.map((item: ListItem) => {
          const id = item.id,
            title = parseListItemPath(item, titlePath),
            subtitle = parseListItemPath(item, subtitlePath),
            headline = parseListItemPath(item, headlinePath),
            keywords: string[] = get(item, keywordsPath),
            url: string = get(item, 'url'),
            summary: string = get(item, 'summary'),
            level: string = get(item, 'level'),
            levelNum: number = get(item, 'levelNum'),
            phone: string = get(item, 'phone'),
            email: string = get(item, 'email'),
            date = formatDateString(get(item, 'date'), dateFormat);

          return (
            <div key={id} id={id} className="grid gap-1">
              <div className="flex items-start justify-between">
                <div className="flex flex-col">
                  {title && <span className="font-semibold">{title}</span>}
                  {subtitle && <span className="opacity-75">{subtitle}</span>}
                </div>

                <div className="flex flex-col gap-1 text-right text-xs">
                  {date && <div className="opacity-50">({date})</div>}
                  {headline && <span className="opacity-75">{headline}</span>}
                </div>
              </div>

              {(level || levelNum > 0) && (
                <div className="grid gap-1">
                  {level && <span className="opacity-75">{level}</span>}
                  {levelNum > 0 && (
                    <div className="level flex">
                      {Array.from(Array(5).keys()).map((_, index) => (
                        <div
                          key={index}
                          className="mr-2 h-3 w-3 rounded-full border"
                          style={{
                            borderColor: 'var(--primary-color)',
                            backgroundColor: levelNum / (10 / 5) > index ? 'var(--primary-color)' : '',
                          }}
                        />
                      ))}
                    </div>
                  )}
                </div>
              )}

              {summary && <Markdown>{summary}</Markdown>}

              {url && (
                <DataDisplay icon={<Link />} link={url}>
                  {url}
                </DataDisplay>
              )}

              {keywords && <div>{keywords.join(', ')}</div>}

              {(phone || email) && (
                <div className="grid gap-1">
                  {phone && (
                    <DataDisplay icon={<Phone />} link={`tel:${phone}`}>
                      {phone}
                    </DataDisplay>
                  )}

                  {email && (
                    <DataDisplay icon={<Email />} link={`mailto:${email}`}>
                      {email}
                    </DataDisplay>
                  )}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </section>
  );
};

export default Section;
